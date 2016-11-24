package com.example.positionassistant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;

import java.io.DataOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.graphics.Path;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

public class RecBuffer implements Runnable {

	private static final String LTAG = "jjTag";
	public boolean stopFlag = false;
	public static int Rcnt = 0;

	//set the record time,the buffer counts
	String countTimes = "";
	int read;
	DataOutputStream os;
//	DataInputStream reader;
	BufferedReader reader;

	// this is in terms of shorts. 10 ms
//	private static final int BUFSIZE = 4096*2;


	// the receiving thread
	private RecBufListener bufReceiver; // assume only one receiver present in
	// the system
	public void setReceiver(RecBufListener rbl)
	{
		this.bufReceiver = rbl;
	}

//	LinkedBlockingQueue<int[]> queue = new LinkedBlockingQueue<int[]>();
//	public RecBuffer(LinkedBlockingQueue<int[]> queue) {
//		this.queue = queue;
//	}

	@Override
	public void run() {
			Log.d(LTAG,"run");
			String res = executeCommand("/system/bin/MyTiny /mnt/sdcard/1 &");
//		Log.d(LTAG,res+"");//返回值为执行该命令后的输出结果
	}

		/**
		 * 执行命令
		 * 执行su产生一个具有root权限的进程：
		   Process p = Runtime.getRuntime().exec("su -");
		   然后，在向这个进程的写入要执行的命令，即可达到以root权限执行命令：
		 */
		public String executeCommand(String...strings) {//类型后跟...，表示此处接受的参数为0到多个Object类型的对象，或者是一个Object[]。
			String res = "";
			DataOutputStream outputStream = null;
			InputStream response = null;
			try{
				Process su = Runtime.getRuntime().exec("su -");
				outputStream = new DataOutputStream(su.getOutputStream());//the output stream to write to the input stream associated with the native process.
				response = su.getInputStream();

//				outputStream.writeBytes("busybox killall MyTiny");

				for (String s : strings) {
					outputStream.writeBytes(s+"\n");
					outputStream.flush();
				}


				outputStream.writeBytes("exit\n");
				outputStream.flush();
				try {
					su.waitFor();//Causes the calling thread to wait for the native process associated with this object to finish executing.
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

//				res = readFully(response);
				readFully(response);
			} catch (IOException e){
				e.printStackTrace();
			} finally {
			}
			return res;
		}

	/**
	 * 存数据
	 */
	public void readFully(InputStream is) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = is.read(buffer)) != -1) {
//			baos.write(buffer, 0, length);

			// call receiver 即RecBufListener
			if (null != this.bufReceiver) {
				if(stopFlag) {
					break;
				}else {
					this.bufReceiver.onRecBufFull(buffer);
					Rcnt++;
//					Log.d(LTAG,"buffer");
				}
			} else {
				Log.d(LTAG,"no one is listening to me. I'm a sad real time recorder");
			}
		}
	}

		/**
	 * kill MyTiny.
	 * @return
	 */
	public boolean stopRecording()
	{

//		if (this.reader == null)
//		{
//			Log.d(LTAG,"try to stop recording thread. But the recording thread is not present.");
//			return false;
//		} else {
			try {
				Process p = Runtime.getRuntime().exec("su");
				DataOutputStream os_kill = new DataOutputStream(p.getOutputStream());

//				os_kill.writeBytes("busybox killall MyTiny\n");
				os_kill.writeBytes("ps MyTiny | busybox awk '{print $2}'|busybox grep -v PID|busybox xargs kill -2\n");
				os_kill.flush();
			} catch (Exception e)

			{
				Log.e(LTAG,"IO error occur when trying to stop recording thread");
				e.printStackTrace();
				return false;
			}
			Log.d(LTAG, "MyTiny killed after recording!");
//		}

//		if (this.os == null)
//		{
//			Log.d(LTAG,"try to stop recording thread. But the recording thread is not present.");
//			return false;
//		} else {
//			try {
//				Process p = Runtime.getRuntime().exec("su");
//				DataOutputStream os_kill = new DataOutputStream(p.getOutputStream());
//
//				os_kill.writeBytes("busybox killall MyTiny\n");
////				os_kill.writeBytes("ps MyTiny | busybox awk '{print $2}'|busybox grep -v PID|busybox xargs kill -2\n");
//				os_kill.flush();
//			} catch (Exception e)
//
//			{
//				Log.e(LTAG,"IO error occur when trying to stop recording thread");
//				e.printStackTrace();
//				return false;
//			}
//			Log.d(LTAG, "MyTiny killed after recording!");
//		}
		return true;
	}
}
