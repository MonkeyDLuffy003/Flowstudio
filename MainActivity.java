package com.flowstudio.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private final int BG=0xFF10151D, PANEL=0xFF1C2532, WHITE=0xFFF5F1EB, MUTED=0xFFA8B3C3, ORANGE=0xFFFFAF66;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout root,body,nav; private TextView status; private EditText idea,story,caption;
    private boolean busy=false; private String tab="Studio"; private android.content.SharedPreferences prefs;
    private final ArrayList<File> scenes=new ArrayList<>();
    private static final String SAMPLE="FOXY’S LAST APPLE\n\n1 · 0–5s · Hook\nFoxy lifts his last apple. A hungry rabbit watches.\nImage prompt: orange fox, brown eyes, cream muzzle, dark paws, white-tipped fluffy tail, orange/brown striped scarf; sunny woodland market; rabbit looking at an apple.\n\n2 · 5–10s · Choice\nFoxy splits the apple and offers half.\nImage prompt: same Foxy and scarf, kneeling beside rabbit, two apple halves, warm light.\n\n3 · 10–15s · Problem\nFoxy’s little cart gets stuck in mud.\nImage prompt: same Foxy pulling a wooden cart, wheel trapped in mud, concerned expression.\n\n4 · 15–20s · Payoff\nRabbit returns with friends. Together they free the cart.\nImage prompt: same Foxy smiling, rabbits pushing cart onto dry path, joyful ending.\n\nCaption: A little kindness goes a long way.";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        prefs=getSharedPreferences("studio",0);
        if(state!=null) { busy=false; }
        File dir=new File(getFilesDir(),"scenes");dir.mkdirs();
        File[] existing=dir.listFiles();if(existing!=null) {Arrays.sort(existing);for(File f:existing) if(f.getName().matches("scene-[0-3]\\.img")) scenes.add(f);}
        show("Studio");
    }
    private int dp(int n) {return (int)(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color) {GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
    private TextView text(String value,int size,int color) {TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(6),0,dp(6));return v;}
    private void heading(String title,String sub) {TextView h=text(title,28,WHITE);h.setTypeface(null,Typeface.BOLD);body.addView(h);body.addView(text(sub,14,MUTED));}
    private void note(String value) {TextView v=text(value,14,MUTED);v.setPadding(dp(16),dp(14),dp(16),dp(14));v.setBackground(background(PANEL));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(10),0,dp(12));body.addView(v,p);}
    private EditText field(String label,String value,boolean multiline) {
        body.addView(text(label,13,ORANGE));EditText e=new EditText(this);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setTextSize(15);e.setText(value);
        e.setBackground(background(PANEL));e.setPadding(dp(14),dp(12),dp(14),dp(12));
        e.setInputType(InputType.TYPE_CLASS_TEXT | (multiline?InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));
        if(multiline) {e.setMinLines(3);e.setGravity(Gravity.TOP);}
        body.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    private void button(String label,Runnable action) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(BG);b.setTextSize(14);b.setBackground(background(ORANGE));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(49));p.setMargins(0,dp(12),0,dp(3));body.addView(b,p);
        b.setOnClickListener(v->{if(busy) {message("A task is running. Please wait.");return;} action.run();});
    }
    private void show(String page) {
        tab=page;root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setPadding(dp(18),dp(12),dp(18),dp(10));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(dp(18)+bars.left,dp(12)+bars.top,dp(18)+bars.right,dp(10)+bars.bottom);
            } else {
                v.setPadding(dp(18)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),dp(18)+insets.getSystemWindowInsetRight(),dp(10)+insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        TextView brand=text("◈  Flowstudio    /    FOXY",18,ORANGE);brand.setTypeface(null,Typeface.BOLD);root.addView(brand);
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(14),0,dp(20));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        status=text(busy?"Working… keep the app open":"PRIVATE WORKSPACE · ON-DEVICE EXPORT",11,MUTED);root.addView(status);
        nav=new LinearLayout(this);
        for(String item:new String[]{"Studio","Queue","Vault","Plan"}) {TextView n=text(item,14,item.equals(page)?ORANGE:MUTED);n.setGravity(Gravity.CENTER);nav.addView(n,new LinearLayout.LayoutParams(0,dp(48),1));n.setOnClickListener(v->{if(busy) {message("Please wait for this task to finish.");return;}saveDraft();if(item.equals("Vault")) unlockVault();else show(item);});}
        root.addView(nav);setContentView(root);root.requestApplyInsets();
        switch(page) {case "Studio":studio();break;case "Queue":queue();break;case "Vault":vault();break;default:plan();}
    }
    private void studio() {
        heading("Small stories.\nBig heart.","Create an original 20-second Foxy Short.");
        note("01  STORY  →  02  SCENES  →  03  EXPORT\nOffline editing and rendering. Gemini is optional. Exports use still scenes, with no audio or AI motion.");
        idea=field("STORY IDEA",prefs.getString("idea","Foxy shares his last apple, then needs help."),true);
        button("Generate story with Gemini",()->{
            saveDraft();
            try {if(new Vault(this).get().isEmpty()) {message("Add your Gemini key in Vault first.");return;}}catch(Exception e){message("Vault is unavailable. Remove and re-add the key.");return;}
            if(System.currentTimeMillis()<prefs.getLong("cooldown",0)) {message("Quota pause active. Retry later after the provider allowance resets.");return;}
            String seed=idea.getText().toString().trim();if(seed.isEmpty()) {message("Enter a story idea.");return;}
            task("Generating story…",()-> {
                try {String result=Gemini.story(new Vault(this).get(),prefs.getString("model","gemini-2.5-flash"),seed);prefs.edit().putString("story",result).apply();}
                catch(Gemini.QuotaException e) {prefs.edit().putLong("cooldown",System.currentTimeMillis()+60*60*1000).apply();throw e;}
            },()->show("Studio"));
        });
        story=field("STORYBOARD · EDIT BEFORE PRODUCING",prefs.getString("story",SAMPLE),true);story.setMinLines(9);
        button("Save draft",()->{saveDraft();message("Draft saved on this device.");});
        caption=field("VIDEO CAPTION · ONE SHORT LINE",prefs.getString("caption","A little kindness goes a long way."),false);
        note("SCENES · "+scenes.size()+" / 4 selected\nChoose up to four images in story order. Images are copied into private app storage. Images are fitted without cropping; each gets equal time.");
        button("Choose scene images",()->{saveDraft();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,100);});
        button("Export 20-second MP4",()->{
            saveDraft();if(scenes.isEmpty()) {message("Choose scene images first.");return;}
            String line=caption.getText().toString().trim();if(line.length()>38) {message("Keep the caption within 38 characters.");return;}
            ArrayList<File> inputs=new ArrayList<>(scenes);File dir=new File(getFilesDir(),"exports");dir.mkdirs();File output=new File(dir,"foxy-"+System.currentTimeMillis()+".mp4");
            task("Rendering · keep Flowstudio open…",()->{
                File partial=new File(dir,output.getName()+".part");
                try {
                    VideoRenderer.render(inputs,line,partial,p->runOnUiThread(()->status.setText("Rendering "+p+"% · keep app open")));
                    if(!partial.renameTo(output))throw new IOException("Could not finalize export.");
                } finally {partial.delete();}
            },()->{show("Queue");message("MP4 ready. Preview before uploading.");});
        });
        button("Add storyboard to queue",()->{
            saveDraft();try {JSONArray list=new JSONArray(prefs.getString("queue","[]"));list.put(new JSONObject().put("id",System.currentTimeMillis()).put("idea",idea.getText().toString()).put("story",story.getText().toString()));prefs.edit().putString("queue",list.toString()).apply();show("Queue");}catch(Exception e){message("Could not save queue.");}
        });
    }
    private void saveDraft() {
        if(tab.equals("Studio")&&idea!=null&&story!=null&&caption!=null) prefs.edit().putString("idea",idea.getText().toString()).putString("story",story.getText().toString()).putString("caption",caption.getText().toString()).apply();
    }
    private void queue() {
        heading("Your production desk","Saved storyboards and finished exports.");
        note("Posting target: 2 Shorts/day. Review each video and upload manually in YouTube. No automatic posting or view guarantees.");
        try {
            JSONArray list=new JSONArray(prefs.getString("queue","[]"));
            if(list.length()==0) note("No queued stories yet. Save a storyboard from Studio.");
            for(int i=0;i<list.length();i++) {JSONObject item=list.getJSONObject(i);final long id=item.getLong("id");note("DRAFT · "+item.optString("idea"));button("Open this storyboard",()->{prefs.edit().putString("story",item.optString("story")).putString("idea",item.optString("idea")).apply();show("Studio");});button("Remove draft",()->{try{JSONArray current=new JSONArray(prefs.getString("queue","[]")),next=new JSONArray();for(int j=0;j<current.length();j++)if(current.getJSONObject(j).getLong("id")!=id)next.put(current.getJSONObject(j));prefs.edit().putString("queue",next.toString()).apply();show("Queue");}catch(Exception e){message("Could not remove draft.");}});}
        }catch(Exception e){note("Queue could not be loaded.");}
        body.addView(text("FINISHED MP4 EXPORTS",14,ORANGE));
        File[] files=new File(getFilesDir(),"exports").listFiles((dir,name)->name.matches("foxy-[0-9]+\\.mp4"));
        if(files==null||files.length==0)note("Your exported videos will appear here.");
        else {Arrays.sort(files,Comparator.comparing(File::getName).reversed());for(File f:files){note(f.getName()+" · "+String.format(Locale.US,"%.1f MB",f.length()/1048576.0));button("Preview video",()->share(f,true));button("Share / save video",()->share(f,false));button("Delete export",()->new AlertDialog.Builder(this).setTitle("Delete this video?").setMessage("This removes the local export permanently.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{if(!f.delete())message("Could not delete video.");show("Queue");}).show());}}
    }
    private void share(File file,boolean preview) {
        Uri uri=Uri.parse("content://com.flowstudio.app.exports/"+file.getName());Intent i=new Intent(preview?Intent.ACTION_VIEW:Intent.ACTION_SEND);
        if(preview)i.setDataAndType(uri,"video/mp4");else{i.setType("video/mp4");i.putExtra(Intent.EXTRA_STREAM,uri);}
        i.setClipData(ClipData.newRawUri("Foxy video",uri));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivity(Intent.createChooser(i,preview?"Preview Foxy":"Save or share your Short"));}catch(ActivityNotFoundException e){message("No compatible video app installed.");}
    }
    private void unlockVault() {
        KeyguardManager manager=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(!manager.isDeviceSecure()) {message("Set a device PIN or password to access the vault.");return;}
        Intent auth=manager.createConfirmDeviceCredentialIntent("Unlock API vault","Confirm your device screen lock.");
        if(auth!=null)startActivityForResult(auth,901);else message("Device authentication unavailable.");
    }
    private void vault() {
        heading("API vault","Your keys stay on this Android device.");
        note("AES-GCM encryption with an Android Keystore key. Backups and screenshots are disabled. Gemini keys are sent only to Google over HTTPS when generating a story. Rooted or compromised devices can still expose secrets.");
        boolean configured=false;try{configured=!new Vault(this).get().isEmpty();}catch(Exception e){note("Vault cannot be decrypted. Delete and re-add your key.");}
        note(configured?"GEMINI · KEY SAVED · ••••••••":"GEMINI · NO KEY CONFIGURED");
        EditText key=field("ADD / REPLACE GEMINI API KEY","",false);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setHint("Paste key, then save");key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        button("Encrypt and save key",()->{String value=key.getText().toString().trim();if(value.isEmpty()){message("Enter a key.");return;}try{new Vault(this).put(value);key.setText("");show("Vault");message("Key encrypted on this device.");}catch(Exception e){message("Secure key storage failed. Key was not saved.");}});
        EditText model=field("GEMINI MODEL",prefs.getString("model","gemini-2.5-flash"),false);
        button("Save model setting",()->{String value=model.getText().toString().trim();if(!value.matches("gemini-[a-zA-Z0-9.\\-]+")){message("Use a Gemini model identifier.");return;}prefs.edit().putString("model",value).apply();message("Model saved. Availability depends on your account.");});
        button("Test provider connection",()->task("Testing provider…",()->{String value=new Vault(this).get();if(value.isEmpty())throw new IOException("Add a key first.");Gemini.story(value,prefs.getString("model","gemini-2.5-flash"),"A friendly fox waves hello.");},()->message("Connection succeeded. This test used provider quota.")));
        button("Delete API key",()->new AlertDialog.Builder(this).setTitle("Delete saved API key?").setMessage("You can add it again later.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{new Vault(this).delete();show("Vault");}).show());
        note("FREE QUOTAS\nNo account or key rotation to evade limits. A quota error pauses generation for at least one hour; the provider may require a longer reset. Offline editing and MP4 export continue to work.");
    }
    private void plan() {
        heading("90 days of Foxy","Build a repeatable craft. Measure what improves.");
        note("DAYS 1–14 · FIND THE FORMAT\nPublish 1–2 original stories daily. Test kindness, clever solutions, and funny mistakes. Check the hook and ending.");
        note("DAYS 15–30 · FOLLOW THE SIGNAL\nCompare median views over 10 uploads, viewed vs swiped away, retention, and subscribers gained. Avoid judging a story immediately after upload.");
        note("DAYS 31–60 · BUILD A SERIES\nAim for 2 strong Shorts daily. Keep Foxy’s design consistent. Add recurring friends and varied plots. Build a small backlog.");
        note("DAYS 61–90 · REFINE\nFocus on your strongest themes. Add a third daily upload only if quality and audience response hold up. No view floor or income is guaranteed.");
        note("AUDIENCE\nSimple English and visual stories can reach US, UK, Canada, and Australia; geography is not guaranteed. Set the children’s audience designation honestly in YouTube Studio.");
        note("PUBLISHING CHECK\nOriginal story • consistent Foxy • licensed images/music • readable caption • satisfying ending • preview video • accurate title • correct audience setting. Review YouTube Studio → Earn for current monetization eligibility.");
        note("PRIVACY\nDrafts, selected images, keys, and exports stay in private app storage. Only your story idea, fixed Foxy description, and API key are sent to Google when you request generation. No analytics, ads, or hidden uploads. Uninstalling clears local data and keys. Export important videos first.");
        button("Delete all local studio data",()->new AlertDialog.Builder(this).setTitle("Erase Flowstudio data?").setMessage("Deletes drafts, scene images, exported videos and your API key. This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Erase",(d,w)->{prefs.edit().clear().commit();new Vault(this).delete();for(String folder:new String[]{"scenes","exports"}){File[] fs=new File(getFilesDir(),folder).listFiles();if(fs!=null)for(File f:fs)f.delete();}scenes.clear();show("Studio");}).show());
    }
    private interface Work {void run() throws Exception;}
    private void task(String label,Work work,Runnable complete) {
        busy=true;status.setText(label);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        worker.submit(()->{String error=null;try{work.run();}catch(Exception e){error=e instanceof Gemini.QuotaException?e.getMessage():safeMessage(e);}final String result=error;runOnUiThread(()->{busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(isFinishing()||isDestroyed())return;status.setText("PRIVATE WORKSPACE · ON-DEVICE EXPORT");if(result==null)complete.run();else message(result);});});
    }
    private String safeMessage(Exception e) {
        if(e instanceof IOException && e.getMessage()!=null && !e.getMessage().contains("http") && !e.getMessage().contains("AIza")) return e.getMessage();
        return "Task failed. Check your connection, model access, and selected images. No secret details were logged.";
    }
    private void message(String value) {new AlertDialog.Builder(this).setMessage(value).setPositiveButton("OK",null).show();}
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==901){if(result==RESULT_OK)show("Vault");else message("Vault stayed locked.");return;}
        if(request!=100||result!=RESULT_OK||data==null)return;
        ArrayList<Uri> picks=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)picks.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)picks.add(data.getData());
        if(picks.isEmpty()||picks.size()>4){message("Select between one and four images.");return;}
        task("Importing scenes…",()->{
            File temp=new File(getCacheDir(),"scene-import");temp.mkdirs();ArrayList<File> imported=new ArrayList<>();
            try {
                for(int i=0;i<picks.size();i++){
                    File f=new File(temp,"scene-"+i+".img");
                    try(InputStream in=getContentResolver().openInputStream(picks.get(i));OutputStream out=new FileOutputStream(f)){
                        if(in==null)throw new IOException("Could not open image.");byte[] buf=new byte[8192];int n;long total=0;
                        while((n=in.read(buf))!=-1){total+=n;if(total>20*1024*1024)throw new IOException("Each scene must be smaller than 20 MB.");out.write(buf,0,n);}
                    }
                    android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(f.getPath(),bounds);
                    if(bounds.outWidth<1||bounds.outHeight<1)throw new IOException("Choose supported JPEG, PNG, or WebP images.");imported.add(f);
                }
                File dest=new File(getFilesDir(),"scenes");for(File f:scenes)f.delete();scenes.clear();
                for(File f:imported){File target=new File(dest,f.getName());try(InputStream in=new FileInputStream(f);OutputStream out=new FileOutputStream(target)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}scenes.add(target);}
            }finally{File[] fs=temp.listFiles();if(fs!=null)for(File f:fs)f.delete();}
        },()->show("Studio"));
    }
    @Override protected void onPause(){saveDraft();super.onPause();}
    @Override protected void onStop(){super.onStop();if(tab.equals("Vault")&&!isFinishing())show("Studio");}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
