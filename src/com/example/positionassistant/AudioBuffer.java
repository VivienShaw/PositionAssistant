package com.example.positionassistant;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.function.dsp.SPUtil;

public class AudioBuffer {
	private final static String MTAG="audioBufferTag";

	//two way audio length
	private int size;
	//sdcard's direction
	private String sdcard;
	//L and R channel data
	private short[] TwoChannels;
	//double型的原始双声道数据
	public double[] double_Raw;
//	FileOutputStream fos;
//	DataOutputStream dos;
	FileWriter fstream;
	BufferedWriter out;


	public AudioBuffer(int buffersize,String sdCard) {
		this.size = buffersize;
		this.sdcard = sdCard;
		TwoChannels = new short[size/2];
		double_Raw = new double[size/2];
//		fos = new FileOutputStream(sdcard+"/MyAppLog/recBufferTest.txt",true);
//		dos = new DataOutputStream(fos);
		try {
			fstream = new FileWriter(sdcard+"/MyAppLog/recBufferTest.txt",true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		out = new BufferedWriter(fstream);
	}

	/*
     * calls: none
     * called by: onRecBufFull
     * TwoChannels
     * save raw file
     */
	public short[] SeperateTwoChannels(short[] data){//data的长度为2048
//		byte[] twoWay;
//		byte[] mic1 = new byte[2];
//		byte[] mic2 = new byte[2];
//
//		int i;
//		int j;
//		int n=0;
//		for (i =0 ; i<data.length;i++){
//			twoWay = intToByteArray(data[i]);
//			for (j=0;j<2;j++)
//			{
//				mic1[j] = twoWay[j];
//			}
//			for (j=0;j<2;j++)
//			{
//				mic2[j] = twoWay[j+2];
//			}
//			TwoChannels[n]=ByteArrayToShort(mic1);
//			TwoChannels[n+1]=ByteArrayToShort(mic2);
//			n=n+2;
//		}

		/*******************smooth the data******************/
//		SPUtil.smooth(TwoChannels);

  /*****************save orignal data in file******************/
		try{
			for (short value:data)
				out.write(value+"\n");

//			for (short value:TwoChannels) {
//				dos.writeShort(value);
//				Log.d("AudioBuffer",value+"");
//				dos.writeBytes("\n");
//			}
		} catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}

		return TwoChannels;
	}

	/*
	 * int transfer to 4 byte
	 */
	public static byte[] intToByteArray(int value) {
		return new byte[] {
				(byte)(value >>> 24),//无符号右移，忽略符号位，空位都以0补齐
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte) value
		};


	}

	/*
	 * 2 Byte transfer to Short
	 */
	public static short ByteArrayToShort(byte[] value){
		short val=(short)( ((value[0]&0xFF)<<8) | (value[1]&0xFF) );
		return val;
	}

	/**
	 * convert an array of short into an array of double.
	 * a deep copy is made
	 * @param data
	 * @return
	 */
	public double[] ShortArrayToDouble(short[] data){
		for (int row=0;row<data.length;row++){
			double_Raw[row]=(double)data[row];
		}
		return double_Raw;
	}

	public void openResource() {
		try {
			fstream = new FileWriter(sdcard+"/MyAppLog/recBufferTest.txt",true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		out = new BufferedWriter(fstream);
	}

	// Close the output stream
	public void closeResource() {
		try {
//			dos.flush();
//			dos.close();
			out.flush();
			out.close();
			fstream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}