package src.swf;

import java.util.*;

public abstract class SWFThread implements Runnable {
    private SWFConfig _config = null;
    private int _number = 0;

    public SWFThread(SWFConfig c, int number) {
        this._config = c;
        this._number = number;
    }

    public Integer getNumber() {
        return this._number;
    }

	public abstract void doWork(SWFConfig c);

	private java.util.List<TaskListener> listeners = Collections.synchronizedList( new ArrayList<TaskListener>() );
	
	public void addListener( TaskListener listener ){
		listeners.add(listener);
    }
    
	public void removeListener( TaskListener listener ){
		listeners.remove(listener);
    }
    
	private final void notifyListeners() {
		synchronized ( listeners ){
			for (TaskListener listener : listeners) {
			  listener.threadComplete(this);
			}
		}
	}

	public void run(){
		doWork(this._config);
		notifyListeners();
	}

}