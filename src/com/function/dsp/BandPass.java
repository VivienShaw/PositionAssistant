package com.function.dsp;

/**
 * Created by vivie on 2016/7/11.
 */
public class BandPass  {
    //用bandFilter类计算出来的滤波器系数
    double[] a = {1.0,-2.3399977147818767,2.865577951933097,-1.8285712206673035,0.6226366754649784};
    double[] b = {0.042195025371812196,0.0,-0.08439005074362439,0.0,0.042195025371812196};

    /// <summary>
    /// Array of input values, latest are in front
    /// </summary>
    private double[] inputHistory = new double[4];

    /// <summary>
    /// Array of output values, latest are in front
    /// </summary>
    private double[] outputHistory = new double[4];

    public void update(int newInput) {
        int newOutput = (int)(newInput * b[0]  +  this.inputHistory[0] * b[1]  +  this.inputHistory[1] * b[2]  + this.inputHistory[2] * b[3] + this.inputHistory[3] * b[4]
                -  this.outputHistory[0] * a[1]  -  this.outputHistory[1] * a[2]  -  this.outputHistory[2] * a[3] - this.outputHistory[3] * a[4]);

        this.inputHistory[3] = this.inputHistory[2];
        this.inputHistory[2] = this.inputHistory[1];
        this.inputHistory[1] = this.inputHistory[0];
        this.inputHistory[0] = newInput;

        this.outputHistory[3] = this.outputHistory[2];
        this.outputHistory[2] = this.outputHistory[1];
        this.outputHistory[1] = this.outputHistory[0];
        this.outputHistory[0] = newOutput;
    }

    public double getValue()
    {
        return this.outputHistory[0];
    }

    //使用java DSP 带通 5K~8K计算出的系数 带通滤波器设置：
    //"bandpass","chebyshev","9","-1","0.1","0.17"
    //filterPassType，filterCharacteristicsType，filterOrder，ripple，fcf1，fcf2
    /*

     y[i] = x[i] * b[0]  +  x[i-1] * b[1]  +  x[i-2] * b[2]  +  ...
                         -  y[i-1] * a[1]  -  y[i-2] * a[2]  -  ...

coeff.a[0]:1.0
coeff.a[1]:-2.3399977147818767
coeff.a[2]:2.865577951933097
coeff.a[3]:-1.8285712206673035
coeff.a[4]:0.6226366754649784

coeff.b[0]:0.042195025371812196
coeff.b[1]:0.0
coeff.b[2]:-0.08439005074362439
coeff.b[3]:0.0
coeff.b[4]:0.042195025371812196

     */
}
