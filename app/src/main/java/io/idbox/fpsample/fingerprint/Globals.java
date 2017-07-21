/* 
 * File: 		Globals.java
 * Created:		2013/05/03
 * 
 * copyright (c) 2013 DigitalPersona Inc.
 */

package io.idbox.fpsample.fingerprint;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;

import com.digitalpersona.uareu.Reader;
import com.digitalpersona.uareu.ReaderCollection;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;
import com.digitalpersona.uareu.Reader.Capabilities;

import android.content.Context;

public class Globals 
{
	public static final Reader.ImageProcessing DefaultImageProcessing = Reader.ImageProcessing.IMG_PROC_DEFAULT;
	//public static final Reader.ImageProcessing DefaultImageProcessing = Reader.ImageProcessing.IMG_PROC_PIV;

	public Reader getReader(String name, Context applContext) throws UareUException 
	{
		getReaders(applContext);

		for (int nCount = 0; nCount < readers.size(); nCount++)
		{
			if (readers.get(nCount).GetDescription().name.equals(name))
			{
				return readers.get(nCount);
			}
		}
		return null;
	}

	public ReaderCollection getReaders(Context applContext) throws UareUException
	{
		readers = UareUGlobal.GetReaderCollection(applContext);
		readers.GetReaders();
		return readers;
	}

	private ReaderCollection readers = null;
	private static Globals instance;

	static 
	{
		instance = new Globals();
	}

	public static Globals getInstance()
	{
		return Globals.instance;
	}

	private static Bitmap m_lastBitmap = null;

	public static void ClearLastBitmap()
	{
		m_lastBitmap = null;
	}

	public static Bitmap GetLastBitmap()
	{
		return m_lastBitmap;
	}

	public static Bitmap GetBitmapFromRaw(byte[] Src, int width, int height)
	{	
		byte [] Bits = new byte[Src.length*4];
		int i = 0;
		for(i=0;i<Src.length;i++)
		{
			Bits[i*4] = Bits[i*4+1] = Bits[i*4+2] = (byte)Src[i];
			Bits[i*4+3] = -1;
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));

		// save bitmap to history to be restored when screen orientation changes
		m_lastBitmap = bitmap;
		return bitmap;
	}
	
	public static final String QualityToString(Reader.CaptureResult result)
	{
		if(result == null)
		{
			return "";
		}
		if(result.quality == null)
		{
			return "An error occurred";
		}
		switch(result.quality)
		{
		case FAKE_FINGER:         return "Fake finger";
		case NO_FINGER:           return "No finger";
		case CANCELED:            return "Capture cancelled";
		case TIMED_OUT:           return "Capture timed out";
		case FINGER_TOO_LEFT:     return "Finger too left";
		case FINGER_TOO_RIGHT:    return "Finger too right";
		case FINGER_TOO_HIGH:     return "Finger too high";
		case FINGER_TOO_LOW:      return "Finger too low";
		case FINGER_OFF_CENTER:   return "Finger off center";
		case SCAN_SKEWED:         return "Scan skewed";
		case SCAN_TOO_SHORT:      return "Scan too short";
		case SCAN_TOO_LONG:       return "Scan too long";
		case SCAN_TOO_SLOW:       return "Scan too slow";
		case SCAN_TOO_FAST:       return "Scan too fast";
		case SCAN_WRONG_DIRECTION:return "Wrong direction";
		case READER_DIRTY:        return "Reader dirty";
		case GOOD:                return "";
		default:                  return "An error occurred";
		}
	}
	public static final int GetFirstDPI(Reader reader)
	{
		Capabilities caps = reader.GetCapabilities();
		return caps.resolutions[0];
	}
}
