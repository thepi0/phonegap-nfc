package com.chariotsolutions.nfc.plugin;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.FormatException; // Cordova 3.x
import android.nfc.NdefMessage;  // Cordova 2.9
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;
import java.io.IOException;
import java.io.StringReader;
import java.io.File;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.InputSource;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NfcPlugin
        extends CordovaPlugin
        implements NfcAdapter.OnNdefPushCompleteCallback
{

    private static final String CONNECT = "connect";
    private static final String CLOSE = "close";
    private static final String TRANSCEIVE = "transceive";
    private static final String GETCARDNUMBER = "getcardnumber";

    private static final String REGISTER_MIME_TYPE = "registerMimeType";
    private static final String REMOVE_MIME_TYPE = "removeMimeType";
    private static final String REGISTER_NDEF = "registerNdef";
    private static final String REMOVE_NDEF = "removeNdef";
    private static final String REGISTER_NDEF_FORMATABLE = "registerNdefFormatable";
    private static final String REGISTER_DEFAULT_TAG = "registerTag";
    private static final String REMOVE_DEFAULT_TAG = "removeTag";
    private static final String WRITE_TAG = "writeTag";
    private static final String ERASE_TAG = "eraseTag";
    private static final String SHARE_TAG = "shareTag";
    private static final String UNSHARE_TAG = "unshareTag";
    private static final String HANDOVER = "handover"; // Android Beam
    private static final String STOP_HANDOVER = "stopHandover";
    private static final String INIT = "init";

    private static final String NDEF = "ndef";
    private static final String NDEF_MIME = "ndef-mime";
    private static final String NDEF_FORMATABLE = "ndef-formatable";
    private static final String TAG_DEFAULT = "tag";

    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";
    private static final String STATUS_NDEF_PUSH_DISABLED = "NDEF_PUSH_DISABLED";

    private static final String ID = "NfcPlugin";
    private final List<IntentFilter> intentFilters = new ArrayList<IntentFilter>();
    private final ArrayList<String[]> techLists = new ArrayList<String[]>();

    private NdefMessage p2pMessage = null;
    private PendingIntent pendingIntent = null;

    private Intent savedIntent = null;

    private CallbackContext shareTagCallback;
    private CallbackContext handoverCallback;

    /**
     * APDU
     */
    private IsoDep isoDep = null;
    private Tag tag = null;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext)
            throws JSONException
    {
        Log.i("TEST", action);

        /*
        if (data.length() == 0)
        {
            Log.d(ID, "execute command " + action);
        }
        else
        {
            Log.d(ID, "execute command " + action + " with " + data.toString());
        }
        

        if (!getNfcStatus().equals(STATUS_NFC_OK))
        {
            callbackContext.error(getNfcStatus());
            return true; // short circuit
        }
        */

        createPendingIntent();

        if (action.equalsIgnoreCase(REGISTER_MIME_TYPE))
        {
            registerMimeType(data, callbackContext);

        }
        else if (action.equalsIgnoreCase(REMOVE_MIME_TYPE))
        {
            removeMimeType(data, callbackContext);

        }
        else if (action.equalsIgnoreCase(REGISTER_NDEF))
        {
            registerNdef(callbackContext);

        }
        else if (action.equalsIgnoreCase(REMOVE_NDEF))
        {
            removeNdef(callbackContext);

        }
        else if (action.equalsIgnoreCase(REGISTER_NDEF_FORMATABLE))
        {
            registerNdefFormatable(callbackContext);

        }
        else if (action.equals(REGISTER_DEFAULT_TAG))
        {
            registerDefaultTag(callbackContext);

        }
        else if (action.equals(REMOVE_DEFAULT_TAG))
        {
            removeDefaultTag(callbackContext);

        }
        else if (action.equalsIgnoreCase(WRITE_TAG))
        {
            writeTag(data, callbackContext);

        }
        else if (action.equalsIgnoreCase(ERASE_TAG))
        {
            eraseTag(callbackContext);

        }
        else if (action.equalsIgnoreCase(SHARE_TAG))
        {
            shareTag(data, callbackContext);

        }
        else if (action.equalsIgnoreCase(UNSHARE_TAG))
        {
            unshareTag(callbackContext);

        }
        else if (action.equalsIgnoreCase(HANDOVER))
        {
            handover(data, callbackContext);

        }
        else if (action.equalsIgnoreCase(STOP_HANDOVER))
        {
            stopHandover(callbackContext);

        }
        else if (action.equalsIgnoreCase(INIT))
        {
            init(callbackContext);

        }
        else if (action.equalsIgnoreCase(CONNECT))
        {
            // APDU
            connect(callbackContext);

        }
        else if (action.equalsIgnoreCase(CLOSE))
        {
            // APDU
            close(callbackContext);

        }
        else if (action.equalsIgnoreCase(TRANSCEIVE))
        {
            // APDU
            transceive(data, callbackContext);

        }
        else if (action.equalsIgnoreCase(GETCARDNUMBER))
        {
            // APDU
            getcardnumber(callbackContext);

        }
        else
        {
            // invalid action
            return false;
        }

        return true;
    }

    /**
     * APDU
     */
    private void connect(final CallbackContext callbackContext)
            throws JSONException
    {
        //Log.e(ID, "## connect ");
        try
        {
            this.cordova.getThreadPool().execute(new NfcConnect(this, callbackContext));
        }
        catch (Throwable e)
        {
            //Log.e(ID, "## EXCEPTION ", e);
            callbackContext.error("COULD_NOT_CONNECT");
        }
    }

    /**
     * APDU
     */
    private void close(CallbackContext callbackContext)
            throws JSONException
    {
        //Log.e(ID, "## close ");
        try
        {
            this.cordova.getThreadPool().execute(new NfcClose(this, callbackContext));
        }
        catch (Throwable e)
        {
            Log.e(ID, "## EXCEPTION ", e);
            callbackContext.error("COULD_NOT_CLOSE");
        }
    }

    /**
     * APDU
     */

     private String BCDtoString(byte[] bytes) {

        StringBuffer number = new StringBuffer();

        for (byte bcd : bytes) {
            StringBuffer sb = new StringBuffer();

            byte high = (byte) (bcd & 0xf0);
            high = (byte) ((high >>> 4) & 0xff);
            high = (byte) (high & 0x0f);
            byte low = (byte) (bcd & 0x0f);

            sb.append(high);
            sb.append(low);

            number.append(sb.toString());
        }

        return number.toString();
    }

    private void getcardnumber(final CallbackContext callbackContext)
            throws JSONException
    {
      cordova.getThreadPool().execute(new Runnable()
      {
          @Override
          public void run()
          {
            try
            {

              if (isoDep == null)
              {
                  //Log.e(ID, "Get card number - No Tech");
                  callbackContext.error("NO_TECH");
              }
              if (!isoDep.isConnected())
              {
                  //Log.e(ID, "Get card number - Not connected");
                  callbackContext.error("NOT_CONNECTED");
              }

              byte[] commandAPDU = {
                (byte) 0x90,
                (byte) 0x5A, // SELECT FILE
                (byte) 0x00, // (byte) 0x04,// Direct selection by DF name
                (byte) 0x00, // Select First record 00 , last re 01 , next record 02
                (byte) 0x03, // length of command data
                (byte) 0x11,
                (byte) 0x20,
                (byte) 0Xef,
                (byte) 0X00 // HSL DFname EF2011
              };

              byte[] responseAPDU = isoDep.transceive(commandAPDU);

              byte[] commandAPDU2 = {
                (byte) 0x90, // CLA
                (byte) 0xBD,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x07,
                (byte) 0x08,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x0b,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00
              };

              byte[] responseAPDU2 = isoDep.transceive(commandAPDU2);
              String cardnumber = BCDtoString(responseAPDU2).substring(2, 20);
              callbackContext.success(cardnumber);

            }
            catch (Throwable e)
            {
                //Log.e(ID, "## EXCEPTION ", e);
                callbackContext.error("ERROR_GETTING_CARD_NUMBER, " + e.getMessage());
            }
          }
      });
    }

    private void transceive(final JSONArray data, final CallbackContext callbackContext)
            throws JSONException
    {
      cordova.getThreadPool().execute(new Runnable()
      {
          @Override
          public void run()
          {
            try
            {

              if (isoDep == null)
              {
                  //Log.e(ID, "Transceive - No Tech");
                  callbackContext.error("NO_TECH");
              }
              if (!isoDep.isConnected())
              {
                  //Log.e(ID, "Transceive - Not connected");
                  callbackContext.error("NOT_CONNECTED");
              }

              String apdustring = data.getString(0);
              Pattern apdus = Pattern.compile("<apdu id=([^<]*)>([^<]*)</apdu>");
              Matcher matcher = apdus.matcher(apdustring);
              String sendback = apdustring;

              while (matcher.find())
              {

                byte[] commandAPDU = hexStringToByteArray(matcher.group(2));
                byte[] responseAPDU = isoDep.transceive(commandAPDU);
                String id = String.format("id=%s", matcher.group(1));
                String replacethis = matcher.group(0);
                String replacewith = "<apdu "+id+">"+byte2Hex(responseAPDU)+"</apdu>";
                sendback = sendback.replaceFirst(replacethis, replacewith);

              }

              callbackContext.success(sendback);

            }
            catch (Throwable e)
            {
                //Log.e(ID, "## EXCEPTION ", e);
                callbackContext.error("ERROR_IN_TRANSCEIVE, " + e.getMessage());
            }
          }
      });
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private byte[] hex2Byte(final String hex)
    {
        return new BigInteger(hex, 16).toByteArray();
    }

    private String byte2Hex(final byte[] b)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++)
        {
            sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    private String getNfcStatus()
    {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null)
        {
            return STATUS_NO_NFC;
        }
        else if (!nfcAdapter.isEnabled())
        {
            return STATUS_NFC_DISABLED;
        }
        else
        {
            return STATUS_NFC_OK;
        }
    }

    private void registerDefaultTag(CallbackContext callbackContext)
    {
        addTagFilter();
        callbackContext.success();
    }

    private void removeDefaultTag(CallbackContext callbackContext)
    {
        removeTagFilter();
        callbackContext.success();
    }

    private void registerNdefFormatable(CallbackContext callbackContext)
    {
        addTechList(new String[]
        {
            NdefFormatable.class.getName()
        });
        callbackContext.success();
    }

    private void registerNdef(CallbackContext callbackContext)
    {
        addTechList(new String[]
        {
            Ndef.class.getName()
        });
        callbackContext.success();
    }

    private void removeNdef(CallbackContext callbackContext)
    {
        removeTechList(new String[]
        {
            Ndef.class.getName()
        });
        callbackContext.success();
    }

    private void unshareTag(CallbackContext callbackContext)
    {
        p2pMessage = null;
        stopNdefPush();
        shareTagCallback = null;
        callbackContext.success();
    }

    private void init(CallbackContext callbackContext)
    {
        Log.d(ID, "Enabling plugin " + getIntent());

        startNfc();
        if (!recycledIntent())
        {
            parseMessage();
        }
        callbackContext.success();
    }

    private void removeMimeType(JSONArray data, CallbackContext callbackContext)
            throws JSONException
    {
        String mimeType = "";
        try
        {
            mimeType = data.getString(0);
            /*boolean removed =*/ removeIntentFilter(mimeType);
            callbackContext.success();
        }
        catch (MalformedMimeTypeException e)
        {
            callbackContext.error("Invalid MIME Type " + mimeType);
        }
    }

    private void registerMimeType(JSONArray data, CallbackContext callbackContext)
            throws JSONException
    {
        String mimeType = "";
        try
        {
            mimeType = data.getString(0);
            intentFilters.add(createIntentFilter(mimeType));
            callbackContext.success();
        }
        catch (MalformedMimeTypeException e)
        {
            callbackContext.error("Invalid MIME Type " + mimeType);
        }
    }

    // Cheating and writing an empty record. We may actually be able to erase some tag types.
    private void eraseTag(CallbackContext callbackContext)
            throws JSONException
    {
        Tag _tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord[] records =
        {
            new NdefRecord(NdefRecord.TNF_EMPTY, new byte[0], new byte[0], new byte[0])
        };
        writeNdefMessage(new NdefMessage(records), _tag, callbackContext);
    }

    private void writeTag(JSONArray data, CallbackContext callbackContext)
            throws JSONException
    {
        if (getIntent() == null)
        {  // TODO remove this and handle LostTag
            callbackContext.error("Failed to write tag, received null intent");
        }

        Tag _tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
        writeNdefMessage(new NdefMessage(records), _tag, callbackContext);
    }

    private void writeNdefMessage(final NdefMessage message, final Tag tag, final CallbackContext callbackContext)
    {
        cordova.getThreadPool().execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Ndef ndef = Ndef.get(tag);
                    if (ndef != null)
                    {
                        ndef.connect();

                        if (ndef.isWritable())
                        {
                            int size = message.toByteArray().length;
                            if (ndef.getMaxSize() < size)
                            {
                                callbackContext.error("Tag capacity is " + ndef.getMaxSize()
                                        + " bytes, message is " + size + " bytes.");
                            }
                            else
                            {
                                ndef.writeNdefMessage(message);
                                callbackContext.success();
                            }
                        }
                        else
                        {
                            callbackContext.error("Tag is read only");
                        }
                        ndef.close();
                    }
                    else
                    {
                        NdefFormatable formatable = NdefFormatable.get(tag);
                        if (formatable != null)
                        {
                            formatable.connect();
                            formatable.format(message);
                            callbackContext.success();
                            formatable.close();
                        }
                        else
                        {
                            callbackContext.error("Tag doesn't support NDEF");
                        }
                    }
                }
                catch (FormatException e)
                {
                    callbackContext.error(e.getMessage());
                }
                catch (TagLostException e)
                {
                    callbackContext.error(e.getMessage());
                }
                catch (IOException e)
                {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void shareTag(JSONArray data, CallbackContext callbackContext)
            throws JSONException
    {
        NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
        this.p2pMessage = new NdefMessage(records);

        startNdefPush(callbackContext);
    }

    // setBeamPushUris
    // Every Uri you provide must have either scheme 'file' or scheme 'content'.
    // Note that this takes priority over setNdefPush
    //
    // See http://developer.android.com/reference/android/nfc/NfcAdapter.html#setBeamPushUris(android.net.Uri[],%20android.app.Activity)
    private void handover(JSONArray data, CallbackContext callbackContext)
            throws JSONException
    {

        Uri[] uri = new Uri[data.length()];

        for (int i = 0; i < data.length(); i++)
        {
            uri[i] = Uri.parse(data.getString(i));
        }

        startNdefBeam(callbackContext, uri);
    }

    private void stopHandover(CallbackContext callbackContext)
            throws JSONException
    {
        stopNdefBeam();
        handoverCallback = null;
        callbackContext.success();
    }

    private void createPendingIntent()
    {
        if (pendingIntent == null)
        {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }

    private void addTechList(String[] list)
    {
        this.addTechFilter();
        this.addToTechList(list);
    }

    private void removeTechList(String[] list)
    {
        this.removeTechFilter();
        this.removeFromTechList(list);
    }

    private void addTechFilter()
    {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private boolean removeTechFilter()
    {
        boolean removed = false;
        Iterator<IntentFilter> iter = intentFilters.iterator();
        while (iter.hasNext())
        {
            IntentFilter intentFilter = iter.next();
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intentFilter.getAction(0)))
            {
                intentFilters.remove(intentFilter);
                removed = true;
            }
        }
        return removed;
    }

    private void addTagFilter()
    {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED));
    }

    private boolean removeTagFilter()
    {
        boolean removed = false;
        Iterator<IntentFilter> iter = intentFilters.iterator();
        while (iter.hasNext())
        {
            IntentFilter intentFilter = iter.next();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intentFilter.getAction(0)))
            {
                intentFilters.remove(intentFilter);
                removed = true;
            }
        }
        return removed;
    }

    private void startNfc()
    {
        createPendingIntent(); // onResume can call startNfc before execute

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null && !getActivity().isFinishing())
                {
                    try
                    {
                        nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), getIntentFilters(), getTechLists());

                        if (p2pMessage != null)
                        {
                            nfcAdapter.setNdefPushMessage(p2pMessage, getActivity());
                        }
                    }
                    catch (IllegalStateException e)
                    {
                        // issue 110 - user exits app with home button while nfc is initializing
                        Log.w(ID, "Illegal State Exception starting NFC. Assuming application is terminating.");
                    }

                }
            }
        });
    }

    private void stopNfc()
    {
        Log.d(ID, "stopNfc");
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null)
                {
                    try
                    {
                        nfcAdapter.disableForegroundDispatch(getActivity());
                    }
                    catch (IllegalStateException e)
                    {
                        // issue 125 - user exits app with back button while nfc
                        Log.w(ID, "Illegal State Exception stopping NFC. Assuming application is terminating.");
                    }
                }
            }
        });
    }

    private void startNdefBeam(final CallbackContext callbackContext, final Uri[] uris)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter == null)
                {
                    callbackContext.error(STATUS_NO_NFC);
                }
                else if (!nfcAdapter.isNdefPushEnabled())
                {
                    callbackContext.error(STATUS_NDEF_PUSH_DISABLED);
                }
                else
                {
                    nfcAdapter.setOnNdefPushCompleteCallback(NfcPlugin.this, getActivity());
                    try
                    {
                        nfcAdapter.setBeamPushUris(uris, getActivity());

                        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                        result.setKeepCallback(true);
                        handoverCallback = callbackContext;
                        callbackContext.sendPluginResult(result);

                    }
                    catch (IllegalArgumentException e)
                    {
                        callbackContext.error(e.getMessage());
                    }
                }
            }
        });
    }

    private void startNdefPush(final CallbackContext callbackContext)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter == null)
                {
                    callbackContext.error(STATUS_NO_NFC);
                }
                else if (!nfcAdapter.isNdefPushEnabled())
                {
                    callbackContext.error(STATUS_NDEF_PUSH_DISABLED);
                }
                else
                {
                    nfcAdapter.setNdefPushMessage(p2pMessage, getActivity());
                    nfcAdapter.setOnNdefPushCompleteCallback(NfcPlugin.this, getActivity());

                    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                    result.setKeepCallback(true);
                    shareTagCallback = callbackContext;
                    callbackContext.sendPluginResult(result);
                }
            }
        });
    }

    private void stopNdefPush()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null)
                {
                    nfcAdapter.setNdefPushMessage(null, getActivity());
                }

            }
        });
    }

    private void stopNdefBeam()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null)
                {
                    nfcAdapter.setBeamPushUris(null, getActivity());
                }

            }
        });
    }

    private void addToTechList(String[] techs)
    {
        techLists.add(techs);
    }

    private void removeFromTechList(String[] techs)
    {
        techLists.remove(techs);
    }

    private boolean removeIntentFilter(String mimeType)
            throws MalformedMimeTypeException
    {
        boolean removed = false;
        Iterator<IntentFilter> iter = intentFilters.iterator();
        while (iter.hasNext())
        {
            IntentFilter intentFilter = iter.next();
            String mt = intentFilter.getDataType(0);
            if (mimeType.equals(mt))
            {
                intentFilters.remove(intentFilter);
                removed = true;
            }
        }
        return removed;
    }

    private IntentFilter createIntentFilter(String mimeType)
            throws MalformedMimeTypeException
    {
        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        intentFilter.addDataType(mimeType);
        return intentFilter;
    }

    private PendingIntent getPendingIntent()
    {
        return pendingIntent;
    }

    private IntentFilter[] getIntentFilters()
    {
        return intentFilters.toArray(new IntentFilter[intentFilters.size()]);
    }

    private String[][] getTechLists()
    {
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return techLists.toArray(new String[0][0]);
    }

    void parseMessage()
    {
        cordova.getThreadPool().execute(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(ID, "parseMessage " + getIntent());
                Intent intent = getIntent();
                String action = intent.getAction();
                Log.d(ID, "action " + action);
                if (action == null)
                {
                    return;
                }

                /*Tag*/ tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Parcelable[] messages = intent.getParcelableArrayExtra((NfcAdapter.EXTRA_NDEF_MESSAGES));

                if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED))
                {
                    Ndef ndef = Ndef.get(tag);
                    fireNdefEvent(NDEF_MIME, ndef, messages);

                }
                else if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED))
                {
                    for (String tagTech : tag.getTechList())
                    {
                        Log.d(ID, tagTech);
                        if (tagTech.equals(NdefFormatable.class.getName()))
                        {
                            fireNdefFormatableEvent(tag);
                        }
                        else if (tagTech.equals(Ndef.class.getName()))
                        { //
                            Ndef ndef = Ndef.get(tag);
                            fireNdefEvent(NDEF, ndef, messages);
                        }
                    }
                }

                if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED))
                {
                    fireTagEvent(tag);
                }

                setIntent(new Intent());
            }
        });
    }

    private void fireNdefEvent(String type, Ndef ndef, Parcelable[] messages)
    {

        JSONObject jsonObject = buildNdefJSON(ndef, messages);
        String tagAsString = jsonObject.toString();

        String command = MessageFormat.format(javaScriptEventTemplate, type, tagAsString);
        Log.v(ID, command);
        this.webView.sendJavascript(command);

    }

    private void fireNdefFormatableEvent(Tag tag)
    {
        String command = MessageFormat.format(javaScriptEventTemplate, NDEF_FORMATABLE, Util.tagToJSON(tag));
        Log.v(ID, command);
        this.webView.sendJavascript(command);
    }

    private void fireTagEvent(Tag tag)
    {
        String command = MessageFormat.format(javaScriptEventTemplate, TAG_DEFAULT, Util.tagToJSON(tag));
        Log.v(ID, command);
        this.webView.sendJavascript(command);
    }

    private void fireConnected(Tag tag)
    {
        Log.e(ID, "fireConnected" + tag);
        String command = MessageFormat.format(javaScriptEventTemplate, "nfc-connected", Util.tagToJSON(tag));
        Log.e(ID, command);
        this.webView.sendJavascript(command);
    }

    JSONObject buildNdefJSON(Ndef ndef, Parcelable[] messages)
    {

        JSONObject json = Util.ndefToJSON(ndef);

        // ndef is null for peer-to-peer
        // ndef and messages are null for ndef format-able
        if (ndef == null && messages != null)
        {

            try
            {

                if (messages.length > 0)
                {
                    NdefMessage message = (NdefMessage) messages[0];
                    json.put("ndefMessage", Util.messageToJSON(message));
                    // guessing type, would prefer a more definitive way to determine type
                    json.put("type", "NDEF Push Protocol");
                }

                if (messages.length > 1)
                {
                    Log.wtf(ID, "Expected one ndefMessage but found " + messages.length);
                }

            }
            catch (JSONException e)
            {
                // shouldn't happen
                Log.e(Util.TAG, "Failed to convert ndefMessage into json", e);
            }
        }
        return json;
    }

    private boolean recycledIntent()
    { // TODO this is a kludge, find real solution

        int flags = getIntent().getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        {
            Log.i(ID, "Launched from history, killing recycled intent");
            setIntent(new Intent());
            return true;
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking)
    {
        Log.d(ID, "onPause " + getIntent());
        super.onPause(multitasking);
        if (multitasking)
        {
            // nfc can't run in background
            stopNfc();
        }
    }

    @Override
    public void onResume(boolean multitasking)
    {
        Log.d(ID, "onResume " + getIntent());
        super.onResume(multitasking);
        startNfc();
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        Log.d(ID, "onNewIntent " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        parseMessage();
    }

    private Activity getActivity()
    {
        return this.cordova.getActivity();
    }

    private Intent getIntent()
    {
        return getActivity().getIntent();
    }

    private void setIntent(Intent intent)
    {
        getActivity().setIntent(intent);
    }

    String javaScriptEventTemplate
            = "var e = document.createEvent(''Events'');\n"
            + "e.initEvent(''{0}'');\n"
            + "e.tag = {1};\n"
            + "document.dispatchEvent(e);";

    @Override
    public void onNdefPushComplete(NfcEvent event)
    {

        // handover (beam) take precedence over share tag (ndef push)
        if (handoverCallback != null)
        {
            PluginResult result = new PluginResult(PluginResult.Status.OK, "Beamed Message to Peer");
            result.setKeepCallback(true);
            handoverCallback.sendPluginResult(result);
        }
        else if (shareTagCallback != null)
        {
            PluginResult result = new PluginResult(PluginResult.Status.OK, "Shared Message with Peer");
            result.setKeepCallback(true);
            shareTagCallback.sendPluginResult(result);
        }

    }

    public Intent getSavedIntent()
    {
        return savedIntent;
    }

    public void setSavedIntent(Intent savedIntent)
    {
        this.savedIntent = savedIntent;
    }

    class NfcConnect
            implements Runnable
    {

        NfcPlugin nfcPlugin;
        CallbackContext callbackContext;

        NfcConnect(NfcPlugin nfcPlugin, CallbackContext callbackContext)
        {
            this.nfcPlugin = nfcPlugin;
            this.callbackContext = callbackContext;
        }

        @Override
        public void run()
        {
            try
            {
                if (nfcPlugin.tag == null)
                {
                    nfcPlugin.tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
                }
                if (nfcPlugin.tag == null)
                {
                    nfcPlugin.tag = (Tag) nfcPlugin.savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                }
                if (nfcPlugin.tag == null)
                {
                    //Log.e(ID, "NFC CONNECT - No Tag");
                    callbackContext.error("NO_TAG");
                }

                nfcPlugin.isoDep = IsoDep.get(tag);
                if (nfcPlugin.isoDep == null)
                {
                    //Log.e(ID, "NFC CONNECT - No Tech");
                    callbackContext.error("NO_TECH");
                }

                //Log.e(ID, "## connect... ");
                nfcPlugin.isoDep.connect();
                nfcPlugin.isoDep.setTimeout(10000);
                //Log.e(ID, "## connected ");

                nfcPlugin.fireConnected(nfcPlugin.tag);

                callbackContext.success();
            }
            catch (IOException ex)
            {
                //Log.e(ID, "ERROR_CONNECTING_ISODEP", ex);
                callbackContext.error("ERROR_CONNECTING_ISODEP, " + ex);
            }
        }
    }

    class NfcClose
            implements Runnable
    {

        NfcPlugin nfcPlugin;
        CallbackContext callbackContext;

        NfcClose(NfcPlugin nfcPlugin, CallbackContext callbackContext)
        {
            this.nfcPlugin = nfcPlugin;
            this.callbackContext = callbackContext;
        }

        @Override
        public void run()
        {
            try
            {
                if (nfcPlugin.isoDep == null)
                {
                    // TODO: no error - just return
                    Log.e(ID, "NFC CLOSE - No Tech");
                    callbackContext.error("NO_TECH");
                }
                if (!isoDep.isConnected())
                {
                    // TODO: no error - just return
                    Log.e(ID, "NFC CLOSE - Not connected");
                    callbackContext.error("NOT_CONNECTED");
                }

                Log.e(ID, "## close... ");
                nfcPlugin.isoDep.close();
                nfcPlugin.isoDep = null;
                Log.e(ID, "## closed ");

//                nfcPlugin.fireClosed(nfcPlugin.tag);
                callbackContext.success();
            }
            catch (IOException ex)
            {
                Log.e(ID, "Can't connect to IsoDep", ex);
            }
        }
    }

    class NfcTransceive
            implements Runnable
    {

        NfcPlugin nfcPlugin;
        JSONArray data;
        CallbackContext callbackContext;

        NfcTransceive(NfcPlugin nfcPlugin, JSONArray data, CallbackContext callbackContext)
        {
            this.nfcPlugin = nfcPlugin;
            this.data = data;
            this.callbackContext = callbackContext;
        }

        @Override
        public void run()
        {
            try
            {
                if (nfcPlugin.isoDep == null)
                {
                    Log.e(ID, "NFC TRANSCEIVE - No Tech");
                    callbackContext.error("NO_TECH");
                }
                if (!nfcPlugin.isoDep.isConnected())
                {
                    Log.e(ID, "NFC TRANSCEIVE - Not connected");
                    callbackContext.error("NOT_CONNECTED");
                }

                Log.e(ID, "hex2byte: " + nfcPlugin.hex2Byte(data.getString(0)));
                //byte[] commandAPDU = nfcPlugin.hex2Byte(data.getString(0));
                byte[] commandAPDU = {
                    (byte) 0x90, // CLA
                    (byte) 0x5A, // INS
                    (byte) 0x0, // P1
                    (byte) 0x0, // P2
                    (byte) 0x03, // Lc
                    (byte) 0x12, // Command Data
                    (byte) 0x20, // Command Data
                    (byte) 0xEF, // Command Data
                    (byte) 0x0 // Le
                };

                byte[] responseAPDU = nfcPlugin.isoDep.transceive(commandAPDU);

                Log.e(ID, "## THIS IS THE CLASS FUNCTION");

                Log.e(ID, "## RECEIVE transceive > " + nfcPlugin.byte2Hex(responseAPDU));

                callbackContext.success(nfcPlugin.byte2Hex(responseAPDU));

                callbackContext.success();
            }
            catch (IOException ex)
            {
                Log.e(ID, "Can't connect to IsoDep", ex);
            }
            catch (JSONException ex)
            {
                Log.e(ID, "Can't get data", ex);
            }
        }
    }
}
