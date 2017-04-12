package demo.recorder.media;

/** 
 * description: describe the class
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
*/
public class MediaObject {
    
    public AudioPart audioPart;

    public VideoPart mVideoPart;

    public AudioPart getAudioPart() {
        return audioPart;
    }

    public void setAudioPart(AudioPart audioPart) {
        this.audioPart = audioPart;
    }


    public VideoPart getmVideoPart() {
        return mVideoPart;
    }

    public void setmVideoPart(VideoPart mVideoPart) {
        this.mVideoPart = mVideoPart;
    }

}
