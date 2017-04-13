package demo.recorder.media;

import demo.recorder.media.audio.AudioPart;

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


    public VideoPart getVideoPart() {
        return mVideoPart;
    }

    public void setVideoPart(VideoPart mVideoPart) {
        this.mVideoPart = mVideoPart;
    }

}
