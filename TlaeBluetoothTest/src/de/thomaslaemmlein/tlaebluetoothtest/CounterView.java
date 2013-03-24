package de.thomaslaemmlein.tlaebluetoothtest;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CounterView extends LinearLayout {
	TextView m_CounterTextView;
	
	private int m_CurrentNumber;
	
	private int m_MaximalNumber = 99;
	
	private INumberReceiver m_NumberReceiver;

	public CounterView(Context context) {
		super(context);
		init();
	}

	public CounterView(Context context, AttributeSet attr) 
	{
		super(context, attr);
		init();
	}
	
	public void SetNumberReceiver(INumberReceiver nr){
		m_NumberReceiver = nr;
	}
	
	private void init()
	{
		Log.d("CounterView", "init");
		
		LayoutInflater inflater = LayoutInflater.from(getContext());
		View root = inflater.inflate(R.layout.counter_view, this, false);
		
	    try
	    {
	    	m_CurrentNumber = 0;
		
	    	m_CounterTextView = (TextView)root.findViewById(R.id.counterTextView);
	    
	    	m_CounterTextView.setText( Integer.toString(m_CurrentNumber));
	    }
	    catch(Exception e){
	    }
	    
	    addView(root);
	}
	
	public void SetNumber(int newNumber)
	{
		if ( newNumber <= m_MaximalNumber && newNumber >= 0){
			m_CurrentNumber = newNumber;
			m_CounterTextView.setText( Integer.toString(m_CurrentNumber));
		}
	}
		
	@Override
	 public boolean onTouchEvent(MotionEvent event) {
		Log.d("CounterView", "onTouchEvent");
		gestureDetector.onTouchEvent(event);
	    return true;
	 }
	
	SimpleOnGestureListener simpleOnGestureListener
	   = new SimpleOnGestureListener(){


	  @Override
	  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
	    float velocityY) {
	   String swipe = "";
	   float sensitvity = 50;
	   
	   // TODO Auto-generated method stub
	   if((e1.getX() - e2.getX()) > sensitvity){
	    swipe += "Swipe Left;";
	   }else if((e2.getX() - e1.getX()) > sensitvity){
	    swipe += "Swipe Right;";
	   }else{
	    swipe += ";";
	   }
	   
	   if((e1.getY() - e2.getY()) > sensitvity){
	    swipe += "Swipe Up;";
	    if (m_CurrentNumber < m_MaximalNumber)
	    {
		    m_CurrentNumber++;
		    m_CounterTextView.setText( Integer.toString(m_CurrentNumber));
		    if ( m_NumberReceiver != null )
		    {
		    	m_NumberReceiver.SetNumber(m_CurrentNumber);
		    }
	    }
	   }else if((e2.getY() - e1.getY()) > sensitvity){
		   if (m_CurrentNumber > 0)
		   {
			   m_CurrentNumber--;
			   m_CounterTextView.setText( Integer.toString(m_CurrentNumber));
    		   if ( m_NumberReceiver != null )
			   {
    			   m_NumberReceiver.SetNumber(m_CurrentNumber);
			   }
		   }
	    swipe += "Swipe Down;";
	   }else{
	    swipe += ";";
	   }
	   
	   Log.d("onFling", swipe);
	   
	   return super.onFling(e1, e2, velocityX, velocityY);
	  }
	   };
	  
	   GestureDetector gestureDetector
	   = new GestureDetector(getContext(), simpleOnGestureListener);
	  
}
