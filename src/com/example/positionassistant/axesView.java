package com.example.positionassistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class axesView extends View {

	public MainActivity myActivity;

	public axesView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public axesView(Context context,AttributeSet attr) {
		super(context,attr);
	}
	private Paint myPaint;
	private static final String title = "TDOA检测两麦克到声源的距离差";


	@Override
	protected void onDraw(Canvas canvas){
		// TODO Auto-generated method stub 
		super.onDraw(canvas);
		myPaint = new Paint();
		//绘制标题
		myPaint.setColor(Color.BLACK); //设置画笔颜色
		myPaint.setTextSize(18);//设置文字大小
		canvas.drawText(title, 20, 20, myPaint);
		//绘制坐标轴
		canvas.drawLine(50, 100, 50, 500, myPaint);//纵坐标轴
		canvas.drawLine(50, 500, 400, 500, myPaint);//横坐标轴
		int[] array1 = new int[]{0, 50, 100, 150, 200, 250, 300, 350};
		//绘制纵坐标刻度
		myPaint.setTextSize(10);//设置文字大小
		canvas.drawText("单位：厘米", 20, 90, myPaint);
		for (int i = 0; i < array1.length; i++) {
			canvas.drawLine(50, 500 - array1[i], 54, 500 - array1[i], myPaint);
			canvas.drawText(array1[i] + "", 20, 500 - array1[i], myPaint);
		}
		//绘制横坐标文字
		String[] array2 = new String[]{"1", "2", "3", "4"};
		for (int i = 0; i < array2.length; i++) {
			canvas.drawText(array2[i], array1[i] + 80, 520, myPaint);
		}
		//绘制条形图
		myPaint.setColor(Color.BLUE); //设置画笔颜色
		myPaint.setStyle(Style.FILL); //设置填充
		canvas.drawRect(new Rect(90, 500 - 1 , 110, 500), myPaint);//画一个矩形,前两个参数是矩形左上角坐标，后两个参数是右下角坐标
		canvas.drawRect(new Rect(140, 500 - 2, 160, 500), myPaint);//第二个矩形
		canvas.drawRect(new Rect(190, 500 - 3, 210, 500), myPaint);//第三个矩形
		canvas.drawRect(new Rect(240, 500 - 4, 260, 500), myPaint);//第四个矩形
		myPaint.setColor(Color.BLACK); //设置画笔颜色
		canvas.drawText("56.32", 88, 500 - 58, myPaint);//第一个矩形的数字说明
		canvas.drawText("98.00", 138, 500 - 100, myPaint);
		canvas.drawText("207.65", 188, 500 - 209, myPaint);
		canvas.drawText("318.30", 238, 500 - 320, myPaint);
		//绘制出处
//	        myPaint.setColor(Color.BLACK); //设置画笔颜色  
//	        myPaint.setTextSize(16);//设置文字大小  
//	        canvas.drawText(myString2, 20, 560, myPaint);  

	}
}

//<com.example.positionassistant.axesView
//android:id="@+id/axesView1"
//android:layout_width="wrap_content"
//android:layout_height="wrap_content" />
