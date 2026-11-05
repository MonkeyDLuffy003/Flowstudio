package com.flowstudio.app;

import android.graphics.*;
import android.media.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

/** Offline still-scene encoder. No network, native libraries, or storage permission. */
final class VideoRenderer {
    interface Progress { void update(int percent); }
    static final int W=720, H=1280, FPS=24, FRAMES=480;
    static void render(List<File> images, String caption, File output, Progress progress) throws Exception {
        if (images.isEmpty() || images.size()>4) throw new IOException("Choose one to four scene images.");
        MediaCodec codec = null; MediaMuxer muxer = null; boolean started = false, success = false, running = false;
        try {
            int color = 0;
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!info.isEncoder()) continue;
                for (String type : info.getSupportedTypes()) if (type.equalsIgnoreCase("video/avc")) {
                    for (int candidate : info.getCapabilitiesForType(type).colorFormats) {
                        if (candidate==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar || candidate==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) { color=candidate; break; }
                    }
                    if (color!=0) { codec=MediaCodec.createByCodecName(info.getName()); break; }
                }
                if (codec!=null) break;
            }
            if (codec==null) throw new IOException("This device has no compatible H.264 byte-buffer encoder.");
            MediaFormat format=MediaFormat.createVideoFormat("video/avc",W,H);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,color); format.setInteger(MediaFormat.KEY_BIT_RATE,4000000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE,FPS); format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start(); running=true;
            muxer=new MediaMuxer(output.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo(); int frame=0, track=-1, current=-1; boolean eos=false, done=false;
            byte[] yuv=null; long deadline=System.nanoTime()+180_000_000_000L;
            while(!done) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Export cancelled");
                if (System.nanoTime()>deadline) throw new IOException("Encoder timed out. Try again with fewer scenes.");
                if (!eos) {
                    int input=codec.dequeueInputBuffer(10000);
                    if(input>=0) {
                        if(frame==FRAMES) { codec.queueInputBuffer(input,0,0,frame*1000000L/FPS,MediaCodec.BUFFER_FLAG_END_OF_STREAM); eos=true; }
                        else {
                            int scene=Math.min(images.size()-1,frame*images.size()/FRAMES);
                            if(scene!=current) { yuv=sceneBytes(images.get(scene),caption,color); current=scene; }
                            ByteBuffer buffer=codec.getInputBuffer(input); buffer.clear();
                            if(buffer.remaining()<yuv.length) throw new IOException("Encoder input buffer is too small.");
                            buffer.put(yuv); codec.queueInputBuffer(input,0,yuv.length,frame*1000000L/FPS,0); frame++;
                            if(frame%24==0) progress.update(frame*100/FRAMES);
                        }
                    }
                }
                int out=codec.dequeueOutputBuffer(info,10000);
                if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if(started) throw new IOException("Encoder changed output format twice");
                    track=muxer.addTrack(codec.getOutputFormat()); muxer.start(); started=true;
                } else if(out>=0) {
                    ByteBuffer data=codec.getOutputBuffer(out);
                    if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0) info.size=0;
                    if(info.size>0) {
                        if(!started) throw new IOException("Encoder output format missing");
                        data.position(info.offset); data.limit(info.offset+info.size); muxer.writeSampleData(track,data,info);
                    }
                    done=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0; codec.releaseOutputBuffer(out,false);
                }
            }
            muxer.stop(); started=false; success=true;
        } finally {
            if(codec!=null) { if(running) try {codec.stop();} catch(Exception ignored) {} codec.release(); }
            if(muxer!=null) { if(started) try {muxer.stop();} catch(Exception ignored) {} muxer.release(); }
            if(!success) output.delete();
        }
    }
    private static byte[] sceneBytes(File image,String caption,int format) throws IOException {
        BitmapFactory.Options bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true; BitmapFactory.decodeFile(image.getPath(),bounds);
        if(bounds.outWidth<1 || bounds.outHeight<1) throw new IOException("Cannot decode scene image");
        BitmapFactory.Options opts=new BitmapFactory.Options(); int sample=1;
        while(bounds.outWidth/sample>W*2 || bounds.outHeight/sample>H*2) sample*=2;
        opts.inSampleSize=sample; Bitmap source=BitmapFactory.decodeFile(image.getPath(),opts);
        if(source==null) throw new IOException("Cannot decode scene image");
        Bitmap target=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888); Canvas canvas=new Canvas(target); canvas.drawColor(Color.BLACK);
        float scale=Math.min((float)W/source.getWidth(),(float)H/source.getHeight());
        float width=source.getWidth()*scale,height=source.getHeight()*scale;
        canvas.drawBitmap(source,null,new RectF((W-width)/2,(H-height)/2,(W+width)/2,(H+height)/2),new Paint(Paint.FILTER_BITMAP_FLAG)); source.recycle();
        if(!caption.trim().isEmpty()) {
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(0xBB10151D); canvas.drawRoundRect(36,1060,684,1170,24,24,paint);
            paint.setColor(Color.WHITE); paint.setTextSize(32); paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); paint.setTextAlign(Paint.Align.CENTER);
            String line=caption.trim(); while(paint.measureText(line)>590 && line.length()>1) line=line.substring(0,line.length()-1);
            canvas.drawText(line,W/2,1127,paint);
        }
        int[] pixels=new int[W*H]; target.getPixels(pixels,0,W,0,0,W,H); target.recycle();
        byte[] data=new byte[W*H*3/2]; int u=W*H,v=u+W*H/4,uv=u;
        boolean planar=format==MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        for(int row=0;row<H;row++) for(int col=0;col<W;col++) {
            int p=pixels[row*W+col], r=(p>>16)&255,g=(p>>8)&255,b=p&255;
            data[row*W+col]=(byte)clamp(((66*r+129*g+25*b+128)>>8)+16);
            if((row&1)==0 && (col&1)==0) {
                byte cb=(byte)clamp(((-38*r-74*g+112*b+128)>>8)+128),cr=(byte)clamp(((112*r-94*g-18*b+128)>>8)+128);
                if(planar) {data[u++]=cb;data[v++]=cr;} else {data[uv++]=cb;data[uv++]=cr;}
            }
        }
        return data;
    }
    private static int clamp(int value) { return Math.max(0,Math.min(255,value)); }
}
