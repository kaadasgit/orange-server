package cn.orangeiot.mqtt;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Created by giovanni on 16/04/2014.
 * QoS transformes methods.
 */
public class QOSUtils {
    public AbstractMessage.QOSType toQos(byte qosByte) {
        AbstractMessage.QOSType qosTmp = AbstractMessage.QOSType.MOST_ONE;
        if(qosByte == 1)
            qosTmp = AbstractMessage.QOSType.LEAST_ONE;
        if(qosByte == 2)
            qosTmp = AbstractMessage.QOSType.EXACTLY_ONCE;
        return qosTmp;
    }
    public byte toByte(AbstractMessage.QOSType qos) {
        switch (qos) {
            case MOST_ONE:
                return 0;
            case LEAST_ONE:
                return 1;
            case EXACTLY_ONCE:
                return 2;
            case RESERVED:
                return 3;
        }
        return 0;
    }
    public Integer toInt(AbstractMessage.QOSType qos) {
        switch (qos) {
            case MOST_ONE:
                return 0;
            case LEAST_ONE:
                return 1;
            case EXACTLY_ONCE:
                return 2;
            case RESERVED:
                return 3;
        }
        return null;
    }
    public AbstractMessage.QOSType toQos(Integer iQos) {
        if(iQos == null)
            return null;
        AbstractMessage.QOSType qosTmp = AbstractMessage.QOSType.MOST_ONE;
        if(iQos == 1)
            qosTmp = AbstractMessage.QOSType.LEAST_ONE;
        if(iQos == 2)
            qosTmp = AbstractMessage.QOSType.EXACTLY_ONCE;
        return qosTmp;
    }

    public int calculatePublishQos(int iSentQos, int iMaxQos) {
        int iOkQos;
        if (iSentQos < iMaxQos)
            iOkQos = iSentQos;
        else
            iOkQos = iMaxQos;
        return iOkQos;
    }
    public AbstractMessage.QOSType calculatePublishQos(AbstractMessage.QOSType sentQos, AbstractMessage.QOSType maxQos) {
        int iSentQos = toInt(sentQos);
        int iMaxQos = toInt(maxQos);
        int iOkQos = calculatePublishQos(iSentQos, iMaxQos);
        AbstractMessage.QOSType okQos = toQos(iOkQos);
        return okQos;
    }
}
