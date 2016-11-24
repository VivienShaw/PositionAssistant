package com.example.positionassistant;

import com.function.dsp.Filter;
import com.function.dsp.SPUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import ca.uol.aig.fftpack.Complex1D;


/**
 * Created by vivie on 2016/7/2.
 */
public class AudioProcess implements Runnable {

    private static final String DTAG = "AudioProcess" ;

    //调试过程的中间参量
    long sepTime;
    long filterTime;
    long fftTime;
    long multiTime;
    long ifftTime;
    long gccTime;

    int dataLength;
    public int DIFF_DIAN = 0;
    public int dex = 0;
    double[] dou_way; //runAudioProcessing
    int n;

    public static final String SDPATH = MainActivity.SDPATH;
    private int BUFSIZE = MainActivity.BUFSIZE;
    private static final int FreqCutOff = 10000;
    private static final double GCCThreshold = 4 * 10 ^ 8;
    private static final double ENERGEThreshold = 5 * 10 ^ 5;
    private static final int SAMPLE_RATE = 48000;
    public boolean stopFlag = false;

    private FileWriter fWriter_gcc;
    private BufferedWriter bWriter_gcc;
    private FileWriter fWriter;
    private BufferedWriter bWriter;
    private FileWriter fstream_diff;
    private BufferedWriter out_diff;

    LinkedBlockingQueue<short[]> queue = new LinkedBlockingQueue<short[]>();
    Filter filter;

    public AudioProcess(LinkedBlockingQueue<short[]> queue) {
        this.queue = queue;
        filter = new Filter(FreqCutOff, SAMPLE_RATE, Filter.PassType.Lowpass, 1);

    }

    /**
     * audio processing. extract features from audio. Add features to KNN.
     */
    public void runAudioProcessing(short[] rawData) {

        if (rawData == null) {
            return;
        }
        long startFunction = System.currentTimeMillis();//获取当前时间

        /***************seperate two channels***************/
        short[] LeftChannel = new short[BUFSIZE / 4];//数据大小：256
        short[] RightChannel = new short[BUFSIZE / 4];//分开后的双声道
        int i=0;
        for (int j = 0; j < rawData.length; j = j + 2) {
            LeftChannel[i] = rawData[j];//2048
            RightChannel[i] = rawData[j + 1];
            i++;
        }
//        long AfterSepTime = System.currentTimeMillis();
//        sepTime = startFunction - AfterSepTime;
        /*********************fft low Filter**************************/
        for (i = 0; i < LeftChannel.length; i++) {
            filter.Update(LeftChannel[i]);
            LeftChannel[i] = (short) filter.getValue();
        }
        for (i = 0; i < RightChannel.length; i++) {
            filter.Update(RightChannel[i]);
            RightChannel[i] = (short) filter.getValue();
        }

//        BandPass bandPass = new BandPass();
//        for (i = 0; i < LeftChannel.length; i++) {
//            bandPass.update(LeftChannel[i]);
//            LeftChannel[i] = (int)bandPass.getValue();
//        }
//        for (i = 0; i < RightChannel.length; i++) {
//            bandPass.update(RightChannel[i]);
//            RightChannel[i] = (int)bandPass.getValue();
//        }

        //获取当前时间
//        long AfterFilterTime = System.currentTimeMillis();
//        filterTime = AfterSepTime - AfterFilterTime;
        /*****************save filtered data in file******************/
        try {
            for (i = 0; i < LeftChannel.length; i++) {
                bWriter.write(LeftChannel[i] + "\n");
                bWriter.write(RightChannel[i] + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //System.exit(1);
        }
        /************************detect point*****************************/
        /*int startLeft = detectPoint(LeftChannel);
        int startRight = detectPoint(RightChannel);
        int startPoint;
        int length = 128;
        if (startLeft > startRight) {
            startPoint = startRight;
        }else {startPoint = startLeft;}
        short[] detectedLeft = new short[length];
        short[] detectedRight = new short[length];
        for (int n=startPoint;n<startPoint+length;n++) {
            detectedLeft[i] = LeftChannel[n];
        }
        for (int n=startPoint;n<startPoint+length;n++) {
            detectedRight[i] = RightChannel[n];
        }*/

        /***********************fft**************************/
//		//返回fft之后的频域所有数据
		Complex1D LeftSpectrum = SPUtil.DoubleFFT(LeftChannel, false);
		Complex1D RightSpectrum = SPUtil.DoubleFFT(RightChannel, false);
//		Log.d(DTAG,"spectrum长度"+LeftSpectrum.x.length);//256
		//获取当前时间
//		long AfterFFTTime = System.currentTimeMillis();
//		fftTime = AfterFilterTime - AfterFFTTime;

//		/***********************cross correlation**************************/
//		/*
//		 * 对应matlab中的语句:
//		 * X1=fft(seg1);X2=fft(seg2);
//		 * G12=X1.*conj(X2);
//		 * GPHAT=G12;
//		 * gcc=fftshift(abs(ifft(GPHAT)));
//		 */
//		//fft1*fft2
		Complex1D GLR = new Complex1D();
		GLR.x = new double[LeftSpectrum.x.length];
		GLR.y = new double[LeftSpectrum.x.length];
		for (i=0;i<LeftSpectrum.x.length;i++)
		{
			GLR.x[i] = LeftSpectrum.x[i]*RightSpectrum.x[i]+LeftSpectrum.y[i]*RightSpectrum.y[i];
			GLR.y[i] = LeftSpectrum.y[i]*RightSpectrum.x[i]-LeftSpectrum.x[i]*RightSpectrum.y[i];
		}
//		long AfterMultiTime = System.currentTimeMillis();
//		multiTime = AfterFFTTime - AfterMultiTime;
//
//		//ifft() GLR -> XCorr
		Complex1D XCorr = new Complex1D();
		XCorr.x = new double[GLR.x.length];
		XCorr.y = new double[GLR.y.length];
		XCorr = SPUtil.ComplexIFFT(GLR);//逆傅里叶变换 返回复数形式的值
		//获取当前时间
//		long AfterIFFTTime = System.currentTimeMillis();
//		ifftTime = AfterMultiTime - AfterIFFTTime;

		// abs()函数  XCorr -> GCC_java 输出值为double型
		double[] GCC_java = new double[XCorr.x.length];
		double[] mag = new double[XCorr.x.length];
		// get the maginude from IFFT output
		for (int index=0;index < XCorr.x.length;index++)
		{
			// magnitude =re*re + im*im
			mag[index] = XCorr.x[index] * XCorr.x[index]
					+ XCorr.y[index] * XCorr.y[index];
			GCC_java[index] = Math.pow(mag[index], 0.5);
		}

		//fftshift()函数  GCC_java -> GCC 输出值为double型
		double[] GCC = SPUtil.fftshift(GCC_java);
//        Log.d(DTAG,"GCC的长度为："+GCC.length); //256
		//获取当前时间
//		long AfterGccTime = System.currentTimeMillis();
//		gccTime = AfterIFFTTime - AfterGccTime;
        try {
            for (i = 0; i < LeftChannel.length; i++) {
                bWriter_gcc.write(GCC[i] + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //System.exit(1);
        }
//
//        /****************find peak，the maximum of GCC**********************/
//		double Max_value=0;
//		int Local = 0;
//		int Width_tolerance = 15;
//		for (int index = GCC.length/2-Width_tolerance;index < GCC.length/2+Width_tolerance;index++)
//		{
//			if (GCC[index]>Max_value) {
//				Max_value=GCC[index];
//                //判断GCC峰值是否大于某阈值，再次排出噪声的干扰计算
//                if (Max_value >= GCCThreshold)
//                {
//                    /*************************find the location of the peak**********************************/
//                    for (int  n= 0; n < GCC.length;n++){
//                        if (GCC[n] == Max_value){
//                            Local = n;
//                            break;
//                        }
//                    }
//                    /**
//                     * if DIFF_DIAN > 0 left在right 的后面
//                     * else left在right 的前面
//                     */
//                    DIFF_DIAN = Local - BUFSIZE/8;
//                    /**
//                     *     save DIFF_DIAN in file
//                     */
//                    try{
//                        out_diff.write(DIFF_DIAN +"\n");
//                    } catch(Exception e){
//                        e.printStackTrace();
//                        System.exit(1);
//                    }
//                }
//                else continue;
//            }
//
//        }


        /**
         *     save program time in file
         */
//		try{
//			FileWriter fstream_osw = new FileWriter(SDPATH+"/MyAppLog/functionTime.txt",true);
//			BufferedWriter osw = new BufferedWriter(fstream_osw);
//			if (dex==0){
//				String header = "function time test result: \n"
//						+"saveRawDataTime"+"   "
//						+"sepTime"+"   "
//						+"filterTime"+"   "
//						+"fftTime"+"   "
//						+"multiTime"+"   "
//						+"ifftTime"+"   "
//						+"gccTime"+"   "
//						+"\n";
//				osw.write(header);
//			}
//
//			osw.write(saveRawDataTime+"   "
//					+sepTime+"   "
//					+filterTime+"   "
//					+fftTime+"   "
//					+multiTime+"   "
//					+ifftTime+"   "
//					+gccTime+"   "
//					+"\n");
//			// Close the output stream
//			osw.close();
//
//		} catch(Exception e){
//			e.printStackTrace();
//			System.exit(1);
//		}

    }

    private int detectPoint(short[] oneChannel) {
        //TODO:每个256点分帧求敲击起始点
        int win = 128;
        int overlapping = 32;//每个声道三段
        int frameNum = (oneChannel.length-win)/overlapping +1;
        short[] energe = new short[frameNum];
        for (int n=0;n<frameNum;n++) {
            for (int i=n*overlapping;i<win+n*overlapping;i++) {
                energe[n] += oneChannel[i]*oneChannel[i];
            }
            if (n>0) {
                if (energe[n] - energe[n-1] >ENERGEThreshold) {
                    return n*overlapping;
                }
            }
        }
        return 0;
    }


    @Override
    public void run() {
        try {
            while (!stopFlag) {
                //从该阻塞队列中读
                runAudioProcessing(queue.take());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void openResource() {
        try {
            //存储数据
            fWriter = new FileWriter(SDPATH+"/MyAppLog/filtedSignal.txt",true);
            bWriter = new BufferedWriter(fWriter);
            fWriter_gcc = new FileWriter(SDPATH+"/MyAppLog/Gcc.txt",true);
            bWriter_gcc = new BufferedWriter(fWriter_gcc);
            fstream_diff = new FileWriter(SDPATH+"/MyAppLog/diffDian.txt",true);
            out_diff = new BufferedWriter(fstream_diff);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        try {
//            //存储滤波后的数据
//            fWriter = new FileWriter(SDPATH+"/MyAppLog/filtedSignal.txt",true);
//            bWriter = new BufferedWriter(fWriter);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void closeResource() {
        try {
            bWriter.flush();
            bWriter.close();
            fWriter.close();
            out_diff.flush();
            out_diff.close();
            fstream_diff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



//    public void onProcessFinish(int[]  data) {
//        for (int n=0; n<data.length; n=n+2) {
//            try {
//                bWriter.write(data[n]+"\n"
//                        +data[n+1]+"\n");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        Toast.makeText(this,"处理完成",Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void registerProcess(AudioProcess r) {
//        r.setReceiver(this);
//    }

        }
