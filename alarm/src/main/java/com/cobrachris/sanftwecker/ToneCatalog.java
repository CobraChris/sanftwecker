package com.cobrachris.sanftwecker;

/**
 * Original procedural alarm sounds. 0-2 intentionally retain the old waveform identity,
 * so previously saved alarms keep their sound across application updates.
 * All oscillators are synthesized locally: no copyrighted audio, download or network.
 */
public final class ToneCatalog {
    public static final String[] LABELS = {
        "Sanftes Glockenspiel", "Reiner weicher Ton", "Tiefe Harmonie",
        "Klassische Glocke", "Sonnenaufgang", "Digital",
        "Waldmorgen", "Sanftes Piano", "Turbo Alarm"
    };
    public static final String[] DESCRIPTIONS = {
        "Warme Glocken, der bisherige Standard",
        "Ruhiger, gleichmäßiger Klang",
        "Tiefes, weiches Klangbild",
        "Glockenschlag mit sanftem Nachhall",
        "Langsam heller werdende Tonfolge",
        "Deutliche elektronische Impulse",
        "Leises Rauschen und Vogelmelodie",
        "Sanfte Klavier-ähnliche Töne",
        "Markanter Alarm für schwere Morgen"
    };
    private static final double TAU=Math.PI*2.0;
    private ToneCatalog(){}

    public static int count(){return LABELS.length;}
    public static boolean valid(int index){return index>=0&&index<count();}
    public static int safe(int index){return valid(index)?index:0;}

    private static double sin(double hz,double seconds){return Math.sin(TAU*hz*seconds);}
    private static double pulse(double phase,double length){
        if(phase>=length)return 0.0;
        double attack=Math.min(1.0,Math.max(0.0,phase)/.035);
        double release=Math.min(1.0,Math.max(0.0,length-phase)/.045);
        return Math.max(0,Math.min(attack,release));
    }
    private static double cyc(double t,double period){
        return t-Math.floor(t/period)*period;
    }

    /** Bounded sample for 22,050 Hz mono PCM. The service controls the external fade. */
    public static double sample(int requested,double time){
        if(!Double.isFinite(time)||time<0)return 0.0;
        switch(safe(requested)){
            case 1: return sin(392.0,time);
            case 2: return .60*sin(262.0,time)+.40*sin(330.0,time);
            case 3: {
                double p=cyc(time,2.8);
                double fade=Math.exp(-2.0*p);
                return fade*(.62*sin(659.25,p)+.24*sin(1318.5,p)+.14*sin(1977.75,p));
            }
            case 4: {
                // An arpeggio with a quiet attack, repeating every 4.8 seconds.
                int index=(int)(time/0.8)%6;
                double[] hz={261.63,329.63,392.0,523.25,659.25,783.99};
                double p=cyc(time,.8);
                double envelope=(1-Math.exp(-p*9.0))*Math.exp(-p*1.25);
                return envelope*(.8*sin(hz[index],time)+.2*sin(2*hz[index],time));
            }
            case 5: {
                double p=cyc(time,2.0);
                double gate=p<.38?pulse(p,.38):p>=.66&&p<1.04?
                        pulse(p-.66,.38):0;
                return gate*(.68*sin(784,time)+.32*sin(988,time));
            }
            case 6: {
                double wind=.12*sin(137.0,time)+.08*sin(143.0,time);
                double p=cyc(time,3.6);
                double bird=p<.7?pulse(p,.7)*.55*sin(920+280*p,time):
                        p>=1.55&&p<2.2?pulse(p-1.55,.65)*.45*
                        sin(1150-170*(p-1.55),time):0;
                return wind+bird;
            }
            case 7: {
                int index=(int)(time/1.15)%4;
                double[] hz={261.63,329.63,392,523.25};
                double p=cyc(time,1.15);
                double decay=Math.exp(-3.3*p);
                return decay*(.76*sin(hz[index],time)+.16*sin(2*hz[index],time)
                        +.08*sin(3*hz[index],time));
            }
            case 8: {
                double p=cyc(time,1.5);
                double gate=p<.26?pulse(p,.26):p>=.37&&p<.63?
                        pulse(p-.37,.26):p>=.74&&p<1.0?pulse(p-.74,.26):0;
                return gate*(.7*sin(880,time)+.3*sin(1100,time));
            }
            default: // Original Sanftes Glockenspiel; no migration of old selections.
                return .65*sin(392.0,time)+.35*sin(523.25,time);
        }
    }
}
