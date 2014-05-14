import java.util.concurrent.locks.Lock;

//single reader, single writer, wait-free queue
public class LamportQueue<T> {
  volatile int head =0, tail = 0;
  T[] items;
  
  
  
  public LamportQueue(int capacity) {
    items = (T[]) new Object[capacity];
    head = 0; tail =0;
  }
  
  public boolean enq(T x){
    if (tail - head == items.length) {
      return false;
    }
    items[tail % items.length]= x;
    tail++;
    return true;
  }
  
  public T deq(){
    if (tail - head ==0) {
    	return null;
    }
    T x = items[head % items.length];
    head++;
    return x;
  }
  
  
  public Boolean empty() {
    return tail == head;
  }
  

}
