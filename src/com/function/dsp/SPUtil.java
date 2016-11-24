package com.function.dsp;

import android.util.Log;

import com.function.dsp.dsp.featureExtraction;

import ca.uol.aig.fftpack.Complex1D;
import ca.uol.aig.fftpack.ComplexDoubleFFT;
import ca.uol.aig.fftpack.RealDoubleFFT;

public class SPUtil {
	//变量初始化
	private static final String LTAG = "xw_function";
	private static int FFTSIZE;//BUFSIZE/4
	private static int SAMPLING_RATE;
	private final static double THRESHOLD = 0.7;

	public SPUtil(int sample_rate,int data_length){
		SPUtil.SAMPLING_RATE = sample_rate;
		SPUtil.FFTSIZE = data_length;
	}

	/**
	 * get MFCC coefficients from fft spectrum
	 *
	 * @param spectrum
	 *            : the fft of signal calculated by "fft" method in this dspUtil
	 *            class
	 * @param dataLength
	 *            : the original length of signal (not the length of spectrum,
	 *            since the spectrum is around a half of the original length)
	 * @return the MFCC coefficient array
	 */
	public static double[] mfcc(double[] spectrum, int dataLength) {
		featureExtraction mFeatureExtraction = new featureExtraction();
		// Mel Filtering
		int cbin[] = mFeatureExtraction
				.fftBinIndices(SAMPLING_RATE, dataLength);
		// get Mel Filterbank
		double fbank[] = mFeatureExtraction.melFilter(spectrum, cbin);
		// Non-linear transformation
		double f[] = mFeatureExtraction.nonLinearTransformation(fbank);
		// Cepstral coefficients
		double cepc[] = mFeatureExtraction.cepCoefficients(f);
		// Add resulting MFCC to array
		double[] mfccFeatures = new double[mFeatureExtraction.numCepstra];
		for (int i = 0; i < mFeatureExtraction.numCepstra; i++) {
			mfccFeatures[i] = cepc[i];
		}
		return mfccFeatures;
	}

	/**
	 * do real fft on data ①
	 *
	 * @param data
	 *            : the signal to do fft on
	 * @param useWindowFunction
	 *            : true -- apply hanning window before doing fft, false --
	 *            don't use any window functions
	 * @return the fft spectrum. 是幅值形式的 The length of fft spectrum returned is length
	 *         of data/2 + 1 if length of data is even, (length of data + 1) /2
	 *         if length of data is odd. This behavior is trying to reduce the
	 *         storage because of the symmetry of fft
	 */
	public static double[] fft(double[] data, boolean useWindowFunction) {

		double[] fftInput = new double[data.length];
        double[] douData = new double[data.length];
        for(int i=0;i<data.length;i++) {
            douData[i] = data[i];
        }
		if (useWindowFunction) {
			int windowSize = data.length;
			double[] windowedInput = applyWindowFunc(douData, hanning(windowSize));
			System.arraycopy(windowedInput, 0, fftInput, 0,
					windowedInput.length);
		} else {
			System.arraycopy(data, 0, fftInput, 0, data.length);
		}
		int dataLength = fftInput.length;
		RealDoubleFFT ftEngine = new RealDoubleFFT(dataLength);

//        Log.d(LTAG, "导入fft的数据长度："+dataLength);//2048

		// coefficient returned: 0 -- 0th bin(real,0); 1,2 -- 1th
		// bin(real,imag); 3,4 -- 2th bin(real,imag)....
		// for even, n -- n/2 bin(real,0)
		ftEngine.ft(fftInput);

		for (int i=0;i<dataLength;i++)
		{
			Log.d(LTAG, "fftInput:"+fftInput[i]);//调试用
		}
		// get the maginude from FFT coefficients
		double[] spectrum;

		if (dataLength % 2 != 0) {// if odd 奇数
			spectrum = new double[(dataLength + 1) / 2];
			spectrum[0] = Math.pow(fftInput[0] * fftInput[0], 0.5);// dc
			// component
			for (int index = 1; index < dataLength; index = index + 2) {
				// magnitude = re*re + im*im
				double mag = fftInput[index] * fftInput[index]
						+ fftInput[index + 1] * fftInput[index + 1];
				spectrum[(index + 1) / 2] = Math.pow(mag, 0.5);
			}
		} else {// if even 偶数
			spectrum = new double[dataLength / 2 + 1];
			spectrum[0] = Math.pow(fftInput[0] * fftInput[0], 0.5);// dc Math.pow(底数,几次方)
			// component.
			// real only
			for (int index = 1; index < dataLength - 1; index = index + 2) {
				// magnitude =re*re + im*im
				double mag = fftInput[index] * fftInput[index]
						+ fftInput[index + 1] * fftInput[index + 1];
				spectrum[(index + 1) / 2] = Math.pow(mag, 0.5);
			}
			// dc component. real only
			spectrum[spectrum.length - 1] = Math.pow(fftInput[dataLength - 1]
					* fftInput[dataLength - 1], 0.5);
		}
		return spectrum;//1025个数 是2048个数的前面一半，后面的数与前面的数对称，所以只需要知道前面的部分
	}

	/**
	 * do real fft on data ②
	 *
	 * @param data
	 *            : the signal to do fft on
	 * @param useWindowFunction
	 *            : true -- apply hanning window before doing fft, false --
	 *            don't use any window functions
	 * @return the fft spectrum.是复数形式的 The length of fft spectrum returned is length
	 *         of data/2 + 1 if length of data is even, (length of data + 1) /2
	 *         if length of data is odd. This behavior is trying to reduce the
	 *         storage because of the symmetry of fft
	 */
	public static Complex1D DoubleFFT(short[] data, boolean useWindowFunction) {

		double[] fftInput = new double[data.length];
        double[] douData = new double[data.length];
        for(int i=0;i<data.length;i++) {
            douData[i] = data[i];
        }
		if (useWindowFunction) {
			int windowSize = data.length;
			double[] windowedInput = applyWindowFunc(douData, hanning(windowSize));
			System.arraycopy(windowedInput, 0, fftInput, 0,
					windowedInput.length);
		} else {
			System.arraycopy(douData, 0, fftInput, 0, douData.length);
		}
		int dataLength = fftInput.length;
		RealDoubleFFT ftEngine = new RealDoubleFFT(dataLength);

//        Log.d(LTAG, "导入fft的数据长度："+dataLength);//2048

		// coefficient returned: 0 -- 0th bin(real,0); 1,2 -- 1th
		// bin(real,imag); 3,4 -- 2th bin(real,imag)....
		// for even, n -- n/2 bin(real,0)
		ftEngine.ft(fftInput);

//        for (int i=0;i<dataLength;i++)
//        {
//        	Log.d(LTAG, "fftInput:"+fftInput[i]);//调试用
//        }
		// get the maginude from FFT coefficients

		Complex1D spectrum;

		if (dataLength % 2 != 0) {// if odd 奇数
			spectrum = new Complex1D();//[(dataLength + 1) / 2]
			spectrum.x = new double[(dataLength + 1) / 2];
			spectrum.x[0] = fftInput[0];// dc
			spectrum.y[0] = 0;
			// component
			for (int index = 1; index < dataLength; index = index + 2) {
				// magnitude = re*re + im*im
				spectrum.x[(index + 1) / 2] = fftInput[index];
				spectrum.y[(index + 1) / 2] = fftInput[index + 1];
			}
		} else {// if even 偶数 
			/*
			 * 最初的代码返回值是全部频谱的一半，但为了后面的逆傅里叶变换，修改代码使返回值为全部频谱。
			 * 修改日期:2016.2.16
			 */
			spectrum = new Complex1D();//[dataLength / 2 + 1]
			spectrum.x = new double[dataLength];//返回值数组大小为2048
			spectrum.y = new double[dataLength];

			spectrum.x[0] = fftInput[0];// dc 0
			spectrum.y[0] = 0;//Math.pow(底数,几次方)
			// component.
			// real only
			for (int index = 1; index < dataLength - 1; index = index + 2) {
				// magnitude =re*re + im*im
				spectrum.x[(index + 1) / 2] = fftInput[index];
				spectrum.y[(index + 1) / 2] = fftInput[index + 1];// 1~1023
			}
			// dc component. real only
			spectrum.x[dataLength / 2] = fftInput[dataLength - 1];// 数组下标1024 ，第1025个数
			spectrum.y[dataLength / 2] = 0;
			//以上代码完成总共1025个数的赋值，是2048个数的前面一半，后面的数与前面的数对称，所以只需要知道前面的部分

			//2016.2.16添加的内容，频谱的后半段
			int lags=dataLength - 3; //2045
			for (int index = 1; index < dataLength - 1; index = index + 2) {//数组下标 2047~1025 
				// magnitude =re*re + im*im
				spectrum.x[(lags + 1) / 2 + dataLength / 2 ] = fftInput[index];
				spectrum.y[(lags + 1) / 2 + dataLength / 2 ] = -fftInput[index + 1];//共轭对称
				lags = lags - 2;
			}
		}
		return spectrum;
	}

	/**
	 * do complex fft on data
	 * @param data
	 * @return the fft spectrum.是复数形式的
	 * 调用ComplexDoubleFFT中的ft(Complex1D x)函数
	 */
	public static Complex1D ComplexFFT(Complex1D data) {

		Complex1D fftInput = new Complex1D();
		fftInput.x = new double[data.x.length];
		fftInput.y = new double[data.y.length];

		for (int i=0;i<data.x.length;i++){
			fftInput.x[i] = data.x[i];
			fftInput.y[i] = data.y[i];
		}

		int dataLength = fftInput.x.length;
		ComplexDoubleFFT ftEngine = new ComplexDoubleFFT(dataLength);

//        Log.d(LTAG, "导入fft的数据长度："+dataLength);//2048

		ftEngine.ft(fftInput);

		// get the maginude from FFT coefficients
		Complex1D spectrum;

		spectrum = new Complex1D();//[dataLength / 2 + 1]
		spectrum.x = new double[dataLength];
		spectrum.y = new double[dataLength];

		//返回值数组大小为2048
		for (int index = 0; index < dataLength; index = index + 1) {
			// magnitude =re*re + im*im
			spectrum.x[index] = fftInput.x[index];
			spectrum.y[index] = fftInput.y[index];
		}

		return spectrum;
	}

	/**
	 * do inverse fft on inputdata
	 * @param inputFFT
	 * @return ifft
	 * 方法原理：http://stackoverflow.com/questions/7406663/inverse-fourier-transform-in-android
	 * The wikipedia article suggests a number of ways to get an inverse transform.
	 * One way is to reverse the order of the input(specifically, swap x[n] with x[N-n]) before performing the transform.
	 * Another is to conjugate your data before and after you perform the transform.
	 * In any case, you will generally need to multiply by a constant factor, to recover your signal at its original amplitude.
	 * In summary: it is quick and easy to use the regular transform to get the inverse,
	 * which is probably why they don't specifically provide one.
	 */
	public static Complex1D ComplexIFFT(Complex1D inputFFT){

		// compute the inverse FFT of x[], assuming its length is a power of 2
		int N = inputFFT.x.length;
		Complex1D tmp1 = new Complex1D();
		tmp1.x = new double[N];
		tmp1.y = new double[N];
		Complex1D tmp2 = new Complex1D();
		tmp2.x = new double[N];
		tmp2.y = new double[N];
		Complex1D tmp3 = new Complex1D();
		tmp3.x = new double[N];
		tmp3.y = new double[N];

		Complex1D ifft = new Complex1D();
		ifft.x = new double[N];
		ifft.y = new double[N];

		// take conjugate
		for (int index = 0; index < N; index = index + 1) {
			// magnitude =re*re + im*im
			tmp1.x[index] = inputFFT.x[index];
			tmp1.y[index] = -inputFFT.y[index];
		}


		// compute forward FFT
		tmp2 = ComplexFFT(tmp1);

		// take conjugate again
		for (int index = 0; index < N; index = index + 1) {
			// magnitude =re*re + im*im
			tmp3.x[index] = tmp2.x[index];
			tmp3.y[index] = -tmp2.y[index];
		}

		// divide by N
		for (int i = 0; i < N; i++) {
			ifft.x[i] = tmp3.x[i] / N;
			ifft.y[i] = tmp3.y[i] / N;
		}

		return ifft;
	}

	/**
	 * matlab中的fftshift()函数，Shift zero-frequency component to center of spectrum
	 * @param data
	 * @return shiftData
	 */
	public static double[] fftshift(double[] data) {

		int m = data.length;
		double[] result = new double[m];
		int m2 = m/2;
		int i;
		for(i = 0; i < m2; i++) {
			result[i+m2]=data[i];
		}
		for(i = 0; i < m2; i++) {
			result[i]=data[i+m2];
		}
		return result;
	}


	/**
	 * find the maximum value in a 2-D array
	 * @return the maximum value in such 2-D array
	 */
	public static double findMax(double[] data){
		double max=data[0];
		for (int row=0;row<data.length;row++){
			if (data[row] > max)
				max=data[row];
		}
		return max;
	}

	/**
	 *  detect start point using short-time energy method
	 * @param data
	 * @return startP
	 * call:findMax()
	 */

	public static int DetectPoint(double[] data){
		double[] Energy = new double[data.length];
		double Max_E = findMax(Energy);
		double[] diff = new double[Energy.length];
		int startP = 0;
		for (int index=0;index<data.length;index++){
			Energy[index] = data[index]*data[index];
		}

		for (int index=0;index<Energy.length;index++){
			Energy[index] = Energy[index]/Max_E;
		}

		for (int index=0;index<Energy.length;index++){
			diff[index] = Math.abs(Energy[index]-Energy[index]);
			if (diff[index]>THRESHOLD){
				startP=index;
				break;//一声道起点帧数
			}
		}
		return startP;
	}



	/**
	 * get the sum of energy over an interval of frequencies 
	 * @param spectrum: the fft spectrum calculated by fft in this class
	 * @param freqResolution: the resolution of fft: fs/N (sampling rate / length of original signal)
	 * @param binLength: the interval of frequency for each bin: i.e. 20 means we sum energy over every 20 Hz
	 * @param binNum: number of energy bins returned: i.e. binLength 20, binNum 30 --- sum over every 20Hz, get 30 numbers (effective frequency 0Hz --- 20*30 Hz)
	 * @return
	 */
	public static double[] fftEnergyBin(double[] spectrum,
										double freqResolution, int binLength, int binNum) {
		double[] energyBin = new double[binNum];
		for (int index = 0; index < spectrum.length; index++) {
			double freq = index * freqResolution + freqResolution / 2.0;
			int binIndex = (int) Math.round(freq / binLength);
			if (binIndex > binNum - 1) {
				break;
			} else {
				energyBin[binIndex] += Math.pow(spectrum[index], 2.0);
			}
		}
		return energyBin;
	}

	/**
	 * apply desired window function to the signal for fft
	 *
	 * @param signal
	 *            : the signal intended to apply the window on
	 * @param window
	 *            : window function
	 * @return the resulting signal after being applied the window
	 */
	private static double[] applyWindowFunc(double[] signal, double[] window) {
		double[] result = new double[signal.length];
		for (int i = 0; i < window.length; i++) {
			result[i] = signal[i] * window[i];
		}
		return result;
	}

	/**
	 * generate hanning window function
	 *
	 * @param windowSize
	 * @return the hanning window of a size "windowSize"
	 */
	private static double[] hanning(int windowSize) {
		double h_wnd[] = new double[windowSize]; // Hanning window
		for (int i = 0; i < windowSize; i++) { // calculate the hanning window
			h_wnd[i] = 0.5 * (1 - Math
					.cos(2.0 * Math.PI * i / (windowSize - 1)));
		}
		return h_wnd;
	}

	/**
	 * smooth the curve
	 */
	public static void smooth(short[] data){
		/*********************smoothy the curve*****************************************/
		final double ALPHA = 0.05;
		int i;
		for(i= 0;i < data.length;i++){
			if(i == 0 || i==1)
				continue;
			else{
				data[i] = (short)((1-ALPHA)*(double)data[i-2]+ ALPHA*(double)data[i]);
			}
		}
	}

}