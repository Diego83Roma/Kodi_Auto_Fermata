package me.aap.fermata.ui.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AddonLibraryActivity extends AppCompatActivity {
    private static final int BG = Color.rgb(12,17,29), PANEL = Color.rgb(25,34,52);
    private LinearLayout page;
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(48,64,88)); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); showLibrary();
    }
    private JSONObject metadata(File folder) {
        try { return new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(folder,"addon.json").toPath()),"UTF-8")); }
        catch (Exception e) { JSONObject j=new JSONObject(); try { j.put("id",folder.getName()); j.put("name",folder.getName()); } catch(Exception ignored){} return j; }
    }
    private File findIcon(File dir,int depth) {
        if(dir==null||depth>6)return null; File[] fs=dir.listFiles(); if(fs==null)return null;
        for(File f:fs) if(f.isFile()) { String n=f.getName().toLowerCase(java.util.Locale.ROOT); if(n.equals("icon.png")||n.equals("addon.png")||n.equals("logo.png"))return f; }
        for(File f:fs)if(f.isDirectory()){File x=findIcon(f,depth+1);if(x!=null)return x;} return null;
    }
    private List<File> packages() {
        File dir=new File(getFilesDir(),"addons"); if(!dir.isDirectory())dir.mkdirs();
        List<File> out=new ArrayList<>(); File[] fs=dir.listFiles();
        if(fs!=null)for(File f:fs)if(f.isDirectory()&&new File(f,"addon.json").isFile())out.add(f);
        out.sort((a,b)->metadata(a).optString("name",a.getName()).compareToIgnoreCase(metadata(b).optString("name",b.getName())));
        return out;
    }
    private Button button(String label, Runnable action, boolean primary) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?Color.rgb(27,119,194):PANEL));
        b.setOnClickListener(v->action.run()); return b;
    }
    private void showLibrary() {
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(20),dp(18),dp(20),dp(24)); page.setBackgroundColor(BG);
        page.addView(button("‹  Indietro",this::finish,false),new LinearLayout.LayoutParams(-2,-2));
        TextView eyebrow=text("LIBRERIA PERSONALE",12,Color.rgb(89,180,255),true); eyebrow.setLetterSpacing(.14f);
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=dp(20);page.addView(eyebrow,ep);
        page.addView(text("I miei Add-on",30,Color.WHITE,true),new LinearLayout.LayoutParams(-1,-2));
        List<File> items=packages(); TextView count=text(items.size()+" pacchetti importati",14,Color.rgb(174,188,208),false);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(5);cp.bottomMargin=dp(18);page.addView(count,cp);
        if(items.isEmpty()) page.addView(text("La libreria è vuota. Scarica un pacchetto dal Catalogo Kodi oppure importa uno ZIP.",16,Color.rgb(190,203,220),false));
        else {
            int sw=(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density);
            int cols=sw>=1000?5:sw>=720?4:sw>=500?3:2;
            int avail=getResources().getDisplayMetrics().widthPixels-dp(40), cw=Math.max(dp(132),(avail-dp(12)*(cols-1))/cols);
            LinearLayout row=null;
            for(int i=0;i<items.size();i++){
                if(i%cols==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(12);page.addView(row,rp);}
                File folder=items.get(i);JSONObject meta=metadata(folder);
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(10),dp(14),dp(10),dp(14));card.setBackground(shape(PANEL,18));
                ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);File icon=findIcon(folder,0);
                if(icon!=null){Bitmap bm=BitmapFactory.decodeFile(icon.getAbsolutePath());if(bm!=null)image.setImageBitmap(bm);}
                if(image.getDrawable()==null){image.setImageResource(android.R.drawable.ic_menu_view);image.setColorFilter(Color.rgb(143,205,255));image.setBackground(shape(Color.rgb(31,62,91),14));image.setPadding(dp(24),dp(24),dp(24),dp(24));}
                card.addView(image,new LinearLayout.LayoutParams(dp(104),dp(104)));
                TextView name=text(meta.optString("name",folder.getName()),14,Color.WHITE,true);name.setGravity(Gravity.CENTER);name.setMaxLines(2);
                LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(9);card.addView(name,np);
                TextView ver=text("Versione "+meta.optString("version","—"),11,Color.rgb(160,179,202),false);ver.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(4);card.addView(ver,vp);
                LinearLayout.LayoutParams tile=new LinearLayout.LayoutParams(cw,dp(190));if(i%cols!=0)tile.leftMargin=dp(12);row.addView(card,tile);
                card.setOnClickListener(v->showDetails(folder));
            }
        }
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(page,new ScrollView.LayoutParams(-1,-2));setContentView(scroll);
    }
    private void showDetails(File folder) {
        JSONObject meta=metadata(folder);page.removeAllViews();
        page.addView(button("‹  Tutti gli Add-on",this::showLibrary,false),new LinearLayout.LayoutParams(-2,-2));
        ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);File path=findIcon(folder,0);
        if(path!=null){Bitmap bm=BitmapFactory.decodeFile(path.getAbsolutePath());if(bm!=null)icon.setImageBitmap(bm);}
        if(icon.getDrawable()==null)icon.setImageResource(android.R.drawable.ic_menu_view);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(132),dp(132));ip.gravity=Gravity.CENTER_HORIZONTAL;ip.topMargin=dp(28);page.addView(icon,ip);
        TextView title=text(meta.optString("name",folder.getName()),26,Color.WHITE,true);title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(16);page.addView(title,tp);
        TextView info=text("ID: "+meta.optString("id",folder.getName())+"\nVersione: "+meta.optString("version","—")+"\nAutore: "+meta.optString("provider","—"),15,Color.rgb(190,203,220),false);
        LinearLayout.LayoutParams infoLp=new LinearLayout.LayoutParams(-1,-2);infoLp.topMargin=dp(18);page.addView(info,infoLp);
        TextView status=text("Pacchetto ZIP importato e verificato. Per eseguirlo serve il motore Kodi: Fermata resta separata e non esegue codice add-on al suo interno.",14,Color.rgb(174,188,208),false);
        LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-1,-2);st.topMargin=dp(22);page.addView(status,st);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(24);page.addView(button("Apri / installa con Kodi separato",()->handoffToKodi(folder,meta),true),bp);
        LinearLayout.LayoutParams backLp=new LinearLayout.LayoutParams(-1,-2);backLp.topMargin=dp(10);page.addView(button("Torna alla libreria",this::showLibrary,false),backLp);
    }
    private void handoffToKodi(File folder,JSONObject meta) {
        String id=meta.optString("id",folder.getName()),version=meta.optString("version","unknown").replaceAll("[^A-Za-z0-9._-]","_");
        File archive=new File(new File(getFilesDir(),"kodi-packages/"+id),version+".zip");
        if(!archive.isFile()){Toast.makeText(this,"ZIP originale non trovato. Reimporta il pacchetto per preparare l'apertura con Kodi.",Toast.LENGTH_LONG).show();return;}
        try{
            ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,archive.getName());values.put(MediaStore.Downloads.MIME_TYPE,"application/zip");
            values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Kodi Auto Fermata");values.put(MediaStore.Downloads.IS_PENDING,1);
            Uri output=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(output==null)throw new Exception("Impossibile preparare il pacchetto per Kodi");
            try(FileInputStream in=new FileInputStream(archive);OutputStream out=getContentResolver().openOutputStream(output,"w")){
                if(out==null)throw new Exception("Impossibile esportare il pacchetto");byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
            }
            ContentValues ready=new ContentValues();ready.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(output,ready,null,null);
            Intent open=new Intent(Intent.ACTION_VIEW);open.setDataAndType(output,"application/zip");open.setPackage("org.xbmc.kodi");open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(open.resolveActivity(getPackageManager())!=null)startActivity(open);
            else{
                Intent launch=getPackageManager().getLaunchIntentForPackage("org.xbmc.kodi");
                if(launch==null)Toast.makeText(this,"Kodi non risulta installato. ZIP esportato in Download/Kodi Auto Fermata.",Toast.LENGTH_LONG).show();
                else{startActivity(launch);Toast.makeText(this,"ZIP esportato in Download/Kodi Auto Fermata. In Kodi usa Add-on > Installa da un file ZIP.",Toast.LENGTH_LONG).show();}
            }
        }catch(Exception e){Toast.makeText(this,"Apertura separata non riuscita: "+(e.getMessage()==null?"errore":e.getMessage()),Toast.LENGTH_LONG).show();}
    }
}
