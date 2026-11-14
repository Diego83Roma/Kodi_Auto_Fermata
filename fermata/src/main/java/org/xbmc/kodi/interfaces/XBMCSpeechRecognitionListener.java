package org.xbmc.kodi.interfaces;
import android.os.Bundle;
import android.speech.RecognitionListener;
public final class XBMCSpeechRecognitionListener implements RecognitionListener {
    native void _onReadyForSpeech(Bundle bundle);
    native void _onError(int error);
    native void _onResults(Bundle bundle);
    public XBMCSpeechRecognitionListener() {}
    @Override public void onReadyForSpeech(Bundle b){_onReadyForSpeech(b);}
    @Override public void onError(int e){_onError(e);}
    @Override public void onResults(Bundle b){_onResults(b);}
    @Override public void onBeginningOfSpeech(){}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){}
    @Override public void onEvent(int e,Bundle b){}
    @Override public void onPartialResults(Bundle b){}
    @Override public void onRmsChanged(float rms){}
}
