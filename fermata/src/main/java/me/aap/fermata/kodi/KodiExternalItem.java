package me.aap.fermata.kodi;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;
import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Base64;
import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import me.aap.fermata.media.lib.BrowsableItemBase;
import me.aap.fermata.media.lib.PlayableItemBase;
import me.aap.fermata.media.pref.BrowsableItemPrefs;
import me.aap.utils.async.FutureSupplier;
/** Kodi PVR channel, favourite or playlist item. */
public final class KodiExternalItem extends PlayableItemBase {
    private static final String PREFIX="kodi:external:";
    private final String title;
    KodiExternalItem(BrowsableItemBase parent, JSONObject data, String title) {
        super(PREFIX+Base64.encodeToString(data.toString().getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING),parent,null);
        this.title=(title==null||title.isEmpty())?"Elemento Kodi":title;
    }
    public static boolean isId(String id){return id!=null&&id.startsWith(PREFIX);}
    public static KodiExternalItem create(BrowsableItemBase parent,String id){
        try{
            byte[] b=Base64.decode(id.substring(PREFIX.length()),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
            JSONObject d=new JSONObject(new String(b,StandardCharsets.UTF_8));
            return new KodiExternalItem(parent,d,d.optString("title",d.optString("label","Elemento Kodi")));
        }catch(Exception e){throw new IllegalArgumentException("ID Kodi non valido",e);}
    }
    public static JSONObject getPayload(String id)throws JSONException{
        byte[] b=Base64.decode(id.substring(PREFIX.length()),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
        return new JSONObject(new String(b,StandardCharsets.UTF_8));
    }
    @Override protected FutureSupplier<String> buildTitle(int n,BrowsableItemPrefs p){return completed(title);}
    @Override protected FutureSupplier<String> buildSubtitle(){return completed("Kodi");}
    @NonNull @Override public FutureSupplier<MediaMetadataCompat> getMediaData(){
        return completed(new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,getId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,title).build());
    }
    @Override public boolean isSeekable(){return false;}
    @Override public boolean isVideo(){return true;}
    @Override public String getOrigId(){return getId();}
    @NonNull @Override public FutureSupplier<Uri> getIconUri(){return completedNull();}
}
