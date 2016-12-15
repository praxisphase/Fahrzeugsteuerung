package fh.praxisphase.fahrzeugsteuerung.utility;

import android.util.Patterns;

/**
 * IpCheck prüft, ob ein String einer IPv4-Adresse, oder einer IPv6-Adresse oder einer URL entspricht
 */
public class IpCheck {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static final String TAG = "IpCheck";

    /**
     * Prüft, ob ein String einer URL entspricht
     *
     * @param ip Zu prüfender String
     * */
    public boolean validateIp(String ip){
        if(ip.matches(Patterns.WEB_URL.pattern())){
            return true;
        }
//        ip v6 prüfung
        else if(ip.contains(":")){
            return validIpV6(ip);
        }
//        ip v4 prüfung
        else if(ip.matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")){
            return validIpV4(ip);
        }
        return false;
    }

    /**
     * Prüft, ob es sich bei dem String um eine IPv6 handelt.
     *
     * @param ip Die zu prüfende IP
     * */
    private boolean validIpV6(String ip) {
        String[] blocks = ip.split("[:]");
//        Eine IpV6 besteht aus max 8 Blöcken
        if(blocks.length <= 8){
            int emptyBlocks = 0;
            for(int i=0; i < blocks.length; i++){
                if(blocks[i].length() > 0) {
                    if(!(blocks[i].matches("[0-9a-f]{1,4}"))) {
                        return false;
                    }
//                    Der letzte Block einer IpV6-Adresse darf einer IpV4-Adresse entsprechen
                    else if(blocks[i].matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}") && (i+1) == blocks.length){
                        return validIpV4(blocks[i]);
                    }
                } else{
//                    Es darf nur einmal eine Folge von 0er Blöcken entfallen
                    emptyBlocks++;
                    if(emptyBlocks > 1){
                        return false;
                    }
                }
            }
            if(!(emptyBlocks == 0 && blocks.length < 8)){
                return true;
            }
        }

        return false;
    }

    /**
     * Prüft, ob es sich bei der IP um eine IPv4 handelt.
     *
     * @param ip Die zu prüfende Ip
     * */
    private boolean validIpV4(String ip){
        String[] blocks = ip.split("[.]");

        for (String  block : blocks) {
            if (Integer.parseInt(block) > 255) {
                return false;
            }
        }
        return true;
    }
}