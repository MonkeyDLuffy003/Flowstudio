package com.flowstudio.app;

import android.app.*;
import android.os.Bundle;
import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import java.io.*;
import java.util.*;

/** Integration checks against real Android Keystore and MediaCodec, no mock crypto. */
public final class SmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args);start(); }
    @Override public void onStart() {
        Bundle results=new Bundle();
        try {
            Context context=getTargetContext();Vault vault=new Vault(context);vault.delete();
            String secret="integration-secret-"+UUID.randomUUID();vault.put(secret);
            if(!secret.equals(vault.get()))throw new AssertionError("Vault roundtrip failed");
            String stored=context.getSharedPreferences("vault",0).getString("gemini","");
            if(stored.contains(secret)||!stored.contains(":"))throw new AssertionError("Vault is not ciphertext");
            vault.delete();if(!vault.get().isEmpty())throw new AssertionError("Vault deletion failed");
            File image=new File(context.getCacheDir(),"test-scene.png"),video=new File(context.getCacheDir(),"test-video.mp4");
            Bitmap bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.rgb(230,120,40));
            try(OutputStream out=new FileOutputStream(image)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
            try {
                VideoRenderer.render(Collections.singletonList(image),"Foxy test",video,p->{});
                MediaMetadataRetriever retriever=new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(video.getPath());
                    long duration=Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    if(duration<19900||duration>20100)throw new AssertionError("Expected 20s video, got "+duration);
                    if(!"720".equals(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)))throw new AssertionError("Wrong width");
                    if(!"1280".equals(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)))throw new AssertionError("Wrong height");
                    Bitmap frame=retriever.getFrameAtTime(1000000);if(frame==null)throw new AssertionError("No decoded video frame");
                    int pixel=frame.getPixel(360,640);frame.recycle();
                    if(Math.abs(Color.red(pixel)-230)>25 || Math.abs(Color.green(pixel)-120)>25 || Math.abs(Color.blue(pixel)-40)>25)throw new AssertionError("YUV color conversion incorrect");
                }finally {retriever.release();}
            }finally {image.delete();video.delete();}
            results.putString("stream","\nVault encryption/deletion and 20-second MP4 decode checks passed.\n");finish(Activity.RESULT_OK,results);
        }catch(Throwable e){results.putString("stream","\nFAIL: "+e.getClass().getSimpleName()+": "+e.getMessage()+"\n");finish(Activity.RESULT_CANCELED,results);}
    }
}
