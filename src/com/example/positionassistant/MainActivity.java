package com.example.positionassistant;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;

import com.function.dsp.SPUtil;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements RecBufListener {

    //static变量
    public static final int BUFSIZE = 1024;
    private static final String DTAG = "XW_Tag";
    public static final String SDPATH = Environment.getExternalStorageDirectory().getPath();
    private static final int SAMPLE_RATE = 48000;
    private static final int DETECTION_MARGIN = 3000;
    private static boolean Has_Detected = false;
    private static int cnt = 0;
    private long startTime;
    private static long usingAllTime;

    //对象初始化
    private Thread recordingThread = null;
    private Thread processThread = null;
    private RecBuffer mBuffer;
    private AudioProcess mProcess;
    private AudioBuffer mAudioBuffer;
    private SPUtil mSPUtil;
//    private FileOutputStream stream;
//    private FileChannel out;
//    private DataOutputStream writer;

    //变量初始化
    private String counts;
    int[] intData;
    short[] shortData;
    LinkedBlockingQueue<short[]> queue;


    //控件初始化
    private Button start;
    private Button stop;
    private EditText recordCount;
    private EditText processTime;
    private EditText sPackageNum;
    private EditText rPackageNum;

    //LineChart
    private LineChart mChart;
    private final int AUDIO = 0;
    private  final int UPDATE_UI = 1;

    //主线程更新UI
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_UI:
                    mChart.notifyDataSetChanged();
                    mChart.invalidate();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.start_button);
        stop = (Button) findViewById(R.id.stop_button);
        stop.setEnabled(false);
        start.setEnabled(true);

        recordCount = (EditText) findViewById(R.id.record_time);//录音时长
        processTime = (EditText) findViewById(R.id.process_time);
        rPackageNum = (EditText) findViewById(R.id.r_package);
        sPackageNum = (EditText) findViewById(R.id.s_package);

        //画图
        mChart = (LineChart)findViewById(R.id.chart_line);
        initialChart(mChart);
        addLineDataSet(mChart);

        /****************audio buffer to store key strokes*****************/
        this.mAudioBuffer = new AudioBuffer(BUFSIZE, SDPATH);

        /****************SPU tool and function*****************/
        this.mSPUtil = new SPUtil(SAMPLE_RATE, BUFSIZE / 4);

        /********************存储获取的原始音频数据给处理线程*********************/
        queue = new LinkedBlockingQueue<short[]>();

        /*******************开始按钮**************************/
        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //在外部设备创建一个文件夹用于存放中间处理数据，取名：MyAppLog
//			String path = SDPATH + "/" + "MyAppLog";
//			File file = new File(path);
//			if(!file.exists())
//				creatSDDir("MyAppLog");
//			else{
//				final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
//				file.renameTo(to);
//				DeleteFile(to);
////			File file = new File(Environment.getExternalStorageDirectory() + "/" + "MyAppLog");
//				creatSDDir("MyAppLog");
//			}
                /****************************save file init****************************/
//                try {
//                    //原始音频数据 byte型
//                    stream = new FileOutputStream("/mnt/sdcard/1.txt");
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                    System.out.println("没有文件");
//                }

//                try {
//                    //存储转换后的数据
//                    out = new FileOutputStream("/mnt/sdcard/2.txt").getChannel();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }


//                try {
//                    writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("/mnt/sdcard/filtedSignal.txt")));
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }

                counts = recordCount.getText().toString();
//                if (counts != "")
//                    mBuffer.countTimes = counts;
                //进入recBuffer中的录音线程
                Log.d(DTAG, "counts:" + counts);

                /**************************recording thread******************************/
                mBuffer = new RecBuffer();
                MainActivity.this.register(mBuffer);

                /****************************process thread****************************/
                mProcess = new AudioProcess(queue);

                //开启录音线程；
                recordingThread = new Thread(mBuffer);
                recordingThread.start();

                //开启处理线程
                processThread = new Thread(mProcess);
                processThread.start();

                mAudioBuffer.openResource();
                mProcess.openResource();

                Log.d(DTAG, "开始录音");
                Toast.makeText(MainActivity.this, "开始测量", Toast.LENGTH_SHORT).show();
                stop.setEnabled(true);
                start.setEnabled(false);
            }

        });

        /*************************停止按钮***************************/
        stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //调试用
			    processTime.setText("处理耗时:"+usingAllTime+"ms");
                rPackageNum.setText("收包数:"+cnt);
                sPackageNum.setText("发包数:"+RecBuffer.Rcnt);

                //关闭 MyTiny
                boolean log = mBuffer.stopRecording();
                if (log) {
                    Log.d(DTAG, "录音结束");
                    Toast.makeText(MainActivity.this, "结束测量", Toast.LENGTH_SHORT).show();
                }

                //关闭processing thread
                Log.d(DTAG, "processingThread status:" + processThread.isAlive());
                mProcess.stopFlag = true;
                SystemClock.sleep(1000);
                Log.d(DTAG, "processingThread status:" + processThread.isAlive());
                processThread.interrupt();
                processThread = null;

                //关闭recoeding Thread
                Log.d(DTAG, "recordingThread status:" + recordingThread.isAlive());
                mBuffer.stopFlag = true;
                SystemClock.sleep(1000);
                Log.d(DTAG, "recordingThread status:" + recordingThread.isAlive());
                recordingThread.interrupt();
                recordingThread = null;

                stop.setEnabled(false);
                start.setEnabled(true);

                //close file
//                try {
//                    stream.flush();
//                    stream.close();
//                } catch (IOException e) {
//                    System.out.println("FileOutputStream未关闭");
//                    e.printStackTrace();
//                }

//                try {
//                    out.close();
//                } catch (IOException e) {
//                    System.out.println("FileChannel未关闭");
//                    e.printStackTrace();
//                }


//				try {
//                    writer.flush();
//					writer.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}

                mAudioBuffer.closeResource();
                mProcess.closeResource();
            }
        });

    }

    @Override
    public void onRecBufFull(final byte[] data)  {//data长度为2048
        // TODO Auto-generated method stub
        cnt++;
		startTime = System.currentTimeMillis();//获取当前时间

        //存裸数据文件
//        try {
//            //Log.d(DTAG, "stream");
//            stream.write(data);
//            Log.d(DTAG,"write");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //验证格式转换后的数据正确性
//        short[] dataShort = new short[data.length / 2]; //512
//        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
//                .asShortBuffer().get(dataShort);
//        //存转换后的数据文件
//        ByteBuffer myByteBuffer = ByteBuffer.allocate(data.length);
//        myByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
//
//        ShortBuffer myShortBuffer = myByteBuffer.asShortBuffer();
//        myShortBuffer.put(dataShort);
//
//        try {
//            out.write(myByteBuffer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if (cnt > 2) //去掉前面的调试信息
        {
//            //byte[] -> int[]
//            intData = new int[BUFSIZE / 4];
//            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
//                    .asIntBuffer().get(intData);

            //byte[] -> short[]
            shortData = new short[BUFSIZE / 2];
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer().get(shortData);

            /*************************判断当前是否是空格键*****************************/
//            double[] doubleData = mAudioBuffer.ShortArrayToDouble(shortData);

            //返回值为short型

            /***************initial two channel***************/
			for (short value:shortData)
			{
				if (value > DETECTION_MARGIN){
					Has_Detected = true;
					break;
				}
                continue;
			}

            /******************run audio process*******************/
			if (Has_Detected)
			{
                mAudioBuffer.SeperateTwoChannels(shortData); //分声道存储到recBufferTest.txt中

                try {
                    //实时绘图 单个麦克风 验证数据转换格式的正确性
                    /*Log.d("vivien","onUpdate");
                    for (int i=0;i<shortData.length;i=i+2) {
                        addEntry(mChart,shortData[i]);
                        Message msg = new Message();
                        msg.what = UPDATE_UI;
                        handler.sendMessage(msg);
                    }*/

                    //放到该阻塞队列中
                    queue.put(shortData);
                    Has_Detected = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
			}

            Has_Detected = false;

            //计算运行时间
			long endTime = System.currentTimeMillis();
			usingAllTime = endTime-startTime;
        }
    }

    @Override
    public void register(RecBuffer r) {
        // TODO Auto-generated method stub
        r.setReceiver(this);
    }

    private void initialChart(LineChart mChart) {
        mChart.setNoDataTextDescription("暂时尚无数据");
        mChart.setTouchEnabled(true);
        // 可拖曳
        mChart.setDragEnabled(true);
        // 可缩放
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        // 图表的注解(只有当数据集存在时候才生效)
        Legend l = mChart.getLegend();
        // 线性，也可是圆
        l.setForm(Legend.LegendForm.LINE);
        // 颜色
        l.setTextColor(Color.CYAN);
        // x坐标轴
        XAxis xl = mChart.getXAxis();
        xl.setTextColor(0xff00897b);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        // 几个x坐标轴之间才绘制？
        xl.setSpaceBetweenLabels(5);
        // 如果false，那么x坐标轴将不可见
        xl.setEnabled(true);
        // 将X坐标轴放置在底部，默认是在顶部。
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        // 图表左边的y坐标轴线
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(0xff37474f);
        // 最大值
//        leftAxis.setAxisMaxValue(50f);
        // 最小值
//        leftAxis.setAxisMinValue(-10f);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = mChart.getAxisRight();
        // 不显示图表的右边y坐标轴线
        rightAxis.setEnabled(false);
    }
    
    // 为LineChart增加LineDataSet
    private void addLineDataSet(LineChart mChart) {
        LineData data = new LineData();
        data.addDataSet(createHighLineDataSet());
        // 数据显示的颜色
        // data.setValueTextColor(Color.WHITE);
        // 先增加一个空的数据，随后往里面动态添加
        mChart.setData(data);
    }

    private ILineDataSet createHighLineDataSet() {
        LineDataSet set = new LineDataSet(null, "RealtimeAudio");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        // 折线的颜色
        set.setColor(Color.BLUE);
        set.setDrawCircles(false);
//        set.setCircleColor(Color.YELLOW);
//        set.setLineWidth(5f);
        // set.setFillAlpha(128);
//        set.setCircleColorHole(Color.BLUE);
//        set.setHighLightColor(Color.GREEN);
//        set.setValueTextColor(Color.RED);
//        set.setValueTextSize(10f);
//        set.setDrawValues(true);

//        set.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value, Entry entry, int dataSetIndex,
//                                            ViewPortHandler viewPortHandler) {
//                DecimalFormat decimalFormat = new DecimalFormat(".0℃");
//                String s = "RealtimeAudio" + decimalFormat.format(value);
//                return s;
//            }
//        });

        return set;
    }

    private void addEntry(LineChart chart,int newData) {

        LineData data = chart.getData();
        data.addXValue((data.getXValCount()) + "");
        //增加实时音频信号
        ILineDataSet highLineDataSet = data.getDataSetByIndex(AUDIO);
        float fh = (float) newData;
        Entry entryh = new Entry(fh, highLineDataSet.getEntryCount());
        data.addEntry(entryh, AUDIO);

        // 当前统计图表中最多在x轴坐标线上显示的总量
        mChart.setVisibleXRangeMaximum(5000);

        mChart.moveViewToX(data.getXValCount() - 5000);
    }


/***************************以下可不看***********************************/


    public void onStartButton(View view) {
        //在外部设备创建一个文件夹用于存放中间处理数据，取名：MyAppLog
//			String path = SDPATH + "/" + "MyAppLog";
//			File file = new File(path);
//			if(!file.exists())
//				creatSDDir("MyAppLog");
//			else{
//				final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
//				file.renameTo(to);
//				DeleteFile(to);
////			File file = new File(Environment.getExternalStorageDirectory() + "/" + "MyAppLog");
//				creatSDDir("MyAppLog");
//			}

        /****************************save file init****************************/
//		try {
//			stream = new FileOutputStream("/mnt/sdcard/1.txt");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			System.out.println("没有文件");
//		}
//
//		try {
//			out = new FileOutputStream("/mnt/sdcard/2.txt").getChannel();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}

//        try {
//            writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("/mnt/sdcard/gcc.txt")));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        counts = recordCount.getText().toString();
        if (counts != "")
            mBuffer.countTimes = counts;
        //进入recBuffer中的录音进程
        Log.d(DTAG, "counts:" + counts);
        recordingThread.start();
        Log.d(DTAG, "开始录音");

        Toast.makeText(this, "开始测量", Toast.LENGTH_SHORT).show();

    }

    public void onStopButton(View view) {
//		stopFlag = true;

        //调试用
//		Log.d(DTAG, "outData的长度"+mBuffer.read/4);
//		test_info.setText("recfull耗时:"+usingAllTime+"ms"
//				+"数据发送个数:"+RecBuffer.Rcnt
//				+"数据接收个数:"+cnt
//				+"bufferOutPut的读取比特数:"+mBuffer.read);
        boolean log = mBuffer.stopRecording();
        if (log) {
            Log.d(DTAG, "录音结束");
            Toast.makeText(this, "结束测量", Toast.LENGTH_SHORT).show();
        }

        while (!recordingThread.isInterrupted()) {
            Log.d(DTAG, "recordingThread status:" + recordingThread.isAlive());
            recordingThread.interrupt();
        }


        //close file
//		try {
//			out.close();
//		} catch (IOException e) {
//			System.out.println("FileChannel未关闭");
//			e.printStackTrace();
//		}
//
//		try {
//			stream.close();
//		} catch (IOException e) {
//			System.out.println("FileOutputStream未关闭");
//			e.printStackTrace();
//		}

//        try {
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//在活动中使用menu
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {//定义菜单响应事件
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Toast.makeText(this, "点击了action_settings", Toast.LENGTH_SHORT).show();
                break;
            case R.id.add_item:
                Toast.makeText(this, "点击了add_item", Toast.LENGTH_SHORT).show();
                break;
            case R.id.remove_item:
                Toast.makeText(this, "点击了remove_item", Toast.LENGTH_SHORT).show();
                break;
        }

        return true;
    }

    //function

    /*
     * obtain editText
     */
    private double ObtainEditText(EditText i) {
        double data = 0.0;
        String str = i.getText().toString();
        if (str != null && str.length() > 0)
            data = Float.parseFloat(str);
        return data;
    }

    /**
     * 创建文件夹路径
     *
     * @param dirName
     * @return
     */
    public File creatSDDir(String dirName) {
        String SDPATH = Environment.getExternalStorageDirectory() + "/";
        File dir = new File(SDPATH + dirName);
        dir.mkdir();
        return dir;
    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file 要删除的根目录
     *             Android递归方式删除某文件夹下的所有文件
     *             http://www.2cto.com/kf/201305/215998.html
     */
    public void DeleteFile(File file) {
        if (file.exists() == false) {
            return;
        } else {
            if (file.isFile()) {
                file.delete();
                return;
            }
            if (file.isDirectory()) {
                File[] childFile = file.listFiles();
                if (childFile == null || childFile.length == 0) {
                    file.delete();
                    return;
                }
                for (File f : childFile) {
                    DeleteFile(f);
                }
                file.delete();
            }
        }
    }

}
