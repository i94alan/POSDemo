package com.braulio.chairman;

import java.util.Locale;


/**
 * Created by 00770 on 2017/6/8.
 */

public class Utility {

    private final static char[] mChars = "0123456789ABCDEF".toCharArray();

    public static final byte XAC_STX_COMMAND = 0x02;
    public static final byte XAC_ETX_COMMAND = 0x03;
    public static final byte XAC_ACK_COMMAND = 0x06;
    public static final byte XAC_NAK_COMMAND = 0x15;
    public static final byte XAC_EOT_COMMAND = 0x04;
    public static final byte XAC_SI_COMMAND = 0x0F;
    public static final byte XAC_SO_COMMAND = 0x0E;
    public static final byte XAC_RS_COMMAND = 0x1E;


    public static final byte[] XAC_ACK = {0x06};
    public static final byte[] XAC_NAK = {0x15};

    public static byte[] getCommand(byte Data){
        byte[] cmd = new byte[4];
        cmd[0] = XAC_STX_COMMAND;
        cmd[1] = Data;
        cmd[2] = XAC_ETX_COMMAND;
        cmd[3] = calcLRC(cmd);
        return cmd;
    }

    public static byte[] getCommand(byte[] Data){
        byte[] cmd = new byte[Data.length + 3];
        cmd[0] = XAC_STX_COMMAND;
        System.arraycopy(Data, 0, cmd, 1, Data.length);
        cmd[Data.length + 1] = XAC_ETX_COMMAND;
        cmd[Data.length + 2] = calcLRC(cmd);
        return cmd;
    };

    private static byte calcLRC(byte[] bData) {
        // TODO Auto-generated method stub
        byte bLRC = 0;
        int index;
        if(bData[0] == 6) {
            index = 2;
        } else {
            index = 1;
        }
        while(index < bData.length - 1) {
            bLRC ^= (byte)(bData[index] & 255);
            ++index;
        }
        return bLRC;
    };

    public static boolean checkLRC(byte[] bData) {
        boolean bLRC = false;
        byte bLRC1 = calcLRC(bData);
        return bLRC1 == bData[bData.length - 1];
    }

    public static int checkInputData(byte[] bData, int length) {
        return bData[0] == XAC_STX_COMMAND && bData[length - 2] == XAC_ETX_COMMAND
                || bData[0] == XAC_SI_COMMAND && bData[length - 2] == XAC_SO_COMMAND
                || bData[0] == XAC_ACK_COMMAND && bData[1] == XAC_STX_COMMAND && bData[length - 2] == XAC_ETX_COMMAND
                || bData[0] == XAC_ACK_COMMAND && bData[1] == XAC_NAK_COMMAND && bData[length - 2] == XAC_SO_COMMAND?(checkLRC(bData)?0:1):
                (findStartPoint(bData, length) && !findEndPointBackward(bData, length)?2:(!findStartPoint(bData, length) && findEndPointBackward(bData, length)?3:(findStartPoint(bData, length) && findEndPointBackward(bData, length)?4:5)));
    }

    public static synchronized byte[] cutArray(byte[] bSrc, int Offset, int Length) {
        byte[] bData = new byte[Length];
        if(bSrc == null) {
            bData = null;
        } else if(Length + Offset <= bSrc.length) {
            System.arraycopy(bSrc, Offset, bData, 0, Length);
        } else {
            bData = null;
        }
        return bData;
    }

    public static boolean findRSPoint(byte[] bData, int length) {
        for(int index = 0; index < length; ++index) {
            if(bData[index] == XAC_RS_COMMAND) {
                return true;
            }
        }
        return false;
    }

    public static int getRSPoint(byte[] bData, int length) {
        int index;
        for(index = 0; index < length && bData[index] != XAC_RS_COMMAND; ++index) {
            ;
        }
        return index;
    }

    public static boolean findStartPoint(byte[] bData, int length) {
        for(int index = 0; index < length; ++index) {
            if(bData[index] == 2 || bData[index] == 15) {
                return true;
            }
        }
        return false;
    }

    public static int getStartPoint(byte[] bData, int length) {
        int index;
        for(index = 0; index < length && bData[index] != 2 && bData[index] != 15; ++index) {
            ;
        }
        return index;
    }

    public static boolean findEndPointBackward(byte[] bData, int length) {
        for(int index = 0; index < length; ++index) {
            if(bData[index] == 3 || bData[index] == 14) {
                return true;
            }
        }
        return false;
    }

    public static int getEndPointBackward(byte[] bData, int length) {
        int index;
        for(index = 0; index < length && bData[index] != 3 && bData[index] != 14; ++index) {
            ;
        }
        return index;
    }


    public static boolean findEndPointForward(byte[] bData, int length) {
        for(int index = length - 1; index > 0; --index) {
            if(bData[index] == 3 || bData[index] == 14) {
                return true;
            }
        }
        return false;
    }


    public static int getEndPointForward(byte[] bData, int length) {
        int index;
        for(index = length - 1; index > 0 && bData[index] != 3 && bData[index] != 14; --index) {
            ;
        }
        return index;
    }

    public static byte[] hexStr2Bytes(String src){

        src = src.trim().replace(" ", "").toUpperCase(Locale.US);

        int m=0,n=0;
        int iLen=src.length()/2;
        byte[] ret = new byte[iLen];

        for (int i = 0; i < iLen; i++){
            m=i*2+1;
            n=m+1;
            ret[i] = (byte)(Integer.decode("0x"+ src.substring(i*2, m) + src.substring(m,n)) & 0xFF);
        }
        return ret;
    }

    public static String byte2HexStr(byte[] b, int iLen){
        StringBuilder sb = new StringBuilder();
        for (int n=0; n<iLen; n++){
            sb.append(mChars[(b[n] & 0xFF) >> 4]);
            sb.append(mChars[b[n] & 0x0F]);
        }
        return sb.toString().trim().toUpperCase(Locale.US);
    }

    public static String byte2HexStrWithSpace(byte[] b, int iLen){
        StringBuilder sb = new StringBuilder();
        for (int n=0; n<iLen; n++){
            sb.append(mChars[(b[n] & 0xFF) >> 4]);
            sb.append(mChars[b[n] & 0x0F]);
            sb.append(' ');
        }
        return sb.toString().trim().toUpperCase(Locale.US);
    }
}
