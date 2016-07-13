package com.function.dsp;

import ca.uol.aig.fftpack.Complex1D;

public class fftHighFilter {
	
	private static int Fs;
	
	private fftHighFilter(int fs){
		fftHighFilter.Fs = fs;
	}
	
	public void HighFilter(double[] data,int FreCutoff){
		int df = Fs/data.length;
		double CutNumber = Math.ceil(FreCutoff/df);
		
		Complex1D rawSpectrum = SPUtil.DoubleFFT(data, false);
		
//		for (i=0;i<LeftSpectrum.x.length;i++){
//		      GLR.x[i] = LeftSpectrum.x[i]*RightSpectrum.x[i]+LeftSpectrum.y[i]*RightSpectrum.y[i];
//		      GLR.y[i] = LeftSpectrum.y[i]*RightSpectrum.x[i]-LeftSpectrum.x[i]*RightSpectrum.y[i];
//		}
	}
	

}
