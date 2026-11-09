package com.cobrachris.sanftwecker;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ToneCatalogTest {
    @Test public void choicesHaveMatchingLabelsAndDescriptions(){
        assertEquals(9,ToneCatalog.count());
        assertEquals(ToneCatalog.LABELS.length,ToneCatalog.DESCRIPTIONS.length);
        for(int i=0;i<ToneCatalog.count();i++){
            assertTrue(ToneCatalog.valid(i));
            assertFalse(ToneCatalog.LABELS[i].trim().isEmpty());
            assertFalse(ToneCatalog.DESCRIPTIONS[i].trim().isEmpty());
        }
        assertFalse(ToneCatalog.valid(-1));
        assertFalse(ToneCatalog.valid(9));
        assertEquals(0,ToneCatalog.safe(500));
    }
    @Test public void oldThreeWaveformsKeepSameMathematicalSamples(){
        double t=0.24681;
        assertEquals(.65*Math.sin(2*Math.PI*392*t)+.35*Math.sin(2*Math.PI*523.25*t),
                ToneCatalog.sample(0,t),1e-12);
        assertEquals(Math.sin(2*Math.PI*392*t),ToneCatalog.sample(1,t),1e-12);
        assertEquals(.6*Math.sin(2*Math.PI*262*t)+.4*Math.sin(2*Math.PI*330*t),
                ToneCatalog.sample(2,t),1e-12);
    }
    @Test public void everyToneProducesBoundedNonSilentPCM(){
        for(int i=0;i<ToneCatalog.count();i++){
            boolean audible=false;
            for(int n=0;n<22050*4;n+=17){
                double value=ToneCatalog.sample(i,n/22050.0);
                assertTrue("Tone "+i+" unbounded",Double.isFinite(value)&&Math.abs(value)<=1.001);
                if(Math.abs(value)>.03)audible=true;
            }
            assertTrue("Tone "+i+" silent",audible);
        }
    }
    @Test public void everyToneHasDistinctSignature(){
        for(int i=0;i<ToneCatalog.count();i++){
            for(int j=i+1;j<ToneCatalog.count();j++){
                double difference=0;
                for(int n=0;n<1200;n++){
                    double t=.25+n/9000.0;
                    difference+=Math.abs(ToneCatalog.sample(i,t)-ToneCatalog.sample(j,t));
                }
                assertTrue("Tones "+i+" & "+j+" indistinguishable",difference>10);
            }
        }
    }
    @Test public void badSampleTimeReturnsSilence(){
        assertEquals(0,ToneCatalog.sample(0,-1),0);
        assertEquals(0,ToneCatalog.sample(0,Double.NaN),0);
        assertEquals(0,ToneCatalog.sample(0,Double.POSITIVE_INFINITY),0);
    }
}
