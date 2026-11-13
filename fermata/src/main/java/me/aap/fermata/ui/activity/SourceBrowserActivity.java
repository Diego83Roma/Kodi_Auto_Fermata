package me.aap.fermata.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.URLUtil;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class SourceBrowserActivity extends AppCompatActivity {
    private WebView web; private ProgressBar progress;
    private static final long MAX_BYTES=100L*1024L*1024L;
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override protected void onCreate(@Nullable Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(Color.rgb(12,17,29));getWindow().setNavigationBarColor(Color.rgb(12,17,29));
        String name=getIntent().getStringExtra("source_name"),url=getIntent().getStringExtra("source_url");
        if(url==null||!(url.startsWith("https://")||url.startsWith("http://"))){finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(12,17,29));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(6),dp(12),dp(6));
        Button back=new Button(this);back.setText("‹");back.setTextSize(22);back.setAllCaps(false);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(52),dp(48)));
        TextView label=new TextView(this);label.setText(name==null?"Sorgente":name);label.setTextColor(Color.WHITE);label.setTextSize(17);label.setMaxLines(1);
        bar.addView(label,new LinearLayout.LayoutParams(0,-2,1));root.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        web=new WebView(this);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setLoadsImagesAutomatically(true);
        web.setWebViewClient(new WebViewClient());web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int p){progress.setProgress(p);progress.setVisibility(p>=100?android.view.View.GONE:android.view.View.VISIBLE);}});
        web.setDownloadListener((downloadUrl,userAgent,disposition,mime,length)->downloadZip(downloadUrl,userAgent,disposition,mime));
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);web.loadUrl(url);
    }
    private void downloadZip(String url,String userAgent,String disposition,String mime){
        String name=URLUtil.guessFileName(url,disposition,mime);if(name==null||name.trim().isEmpty())name="addon-"+System.currentTimeMillis()+".zip";
        if(!name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")&&(mime==null||!mime.toLowerCase(java.util.Locale.ROOT).contains("zip"))){Toast.makeText(this,"La sorgente non ha restituito un archivio ZIP.",Toast.LENGTH_LONG).show();return;}
        if(!name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))name+=".zip";name=name.replaceAll("[^A-Za-z0-9._-]","_");
        File dir=new File(getFilesDir(),"kodi-packages/sources");if(!dir.isDirectory()&&!dir.mkdirs()){Toast.makeText(this,"Cartella pacchetti non disponibile",Toast.LENGTH_LONG).show();return;}
        File target=new File(dir,name);if(target.exists())target=new File(dir,System.currentTimeMillis()+"-"+name);File temp=new File(target.getAbsolutePath()+".part");
        final String saved=target.getName();Toast.makeText(this,"Download ZIP nella libreria interna…",Toast.LENGTH_SHORT).show();
        new Thread(()->{HttpURLConnection conn=null;try{
            URLConnection raw=new URL(url).openConnection();if(!(raw instanceof HttpURLConnection))throw new Exception("Protocollo non supportato");
            conn=(HttpURLConnection)raw;conn.setConnectTimeout(15000);conn.setReadTimeout(30000);conn.setInstanceFollowRedirects(true);
            if(userAgent!=null)conn.setRequestProperty("User-Agent",userAgent);String cookie=CookieManager.getInstance().getCookie(url);if(cookie!=null)conn.setRequestProperty("Cookie",cookie);
            int code=conn.getResponseCode();if(code<200||code>=300)throw new Exception("HTTP "+code);if(conn.getContentLengthLong()>MAX_BYTES)throw new Exception("ZIP oltre 100 MB");
            try(java.io.InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(temp)){byte[] b=new byte[16384];long total=0;int n;while((n=in.read(b))!=-1){total+=n;if(total>MAX_BYTES)throw new Exception("ZIP oltre 100 MB");out.write(b,0,n);}out.getFD().sync();}
            if(!temp.renameTo(target))throw new Exception("Impossibile salvare il pacchetto");
            runOnUiThread(()->Toast.makeText(this,"ZIP salvato nella libreria Fermata: "+saved,Toast.LENGTH_LONG).show());
        }catch(Exception e){temp.delete();String msg=e.getMessage()==null?"Download non riuscito":e.getMessage();runOnUiThread(()->Toast.makeText(this,msg,Toast.LENGTH_LONG).show());}
        finally{if(conn!=null)conn.disconnect();}}).start();
    }
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
