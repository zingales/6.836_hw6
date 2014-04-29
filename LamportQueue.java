import java.util.concurrent.locks.Lock;

//single reader, single writer, wait-free queue
public class LamportQueue<T> {
  volatile int head =0, tail = 0;
  T[] items;
  
  
  
  public LamportQueue(int capacity, Lock lock) {
    items = (T[]) new Object[capacity];
    head = 0; tail =0;
  }
  
  public void enq(T x) throws FullException{
    if (tail - head == items.length) {
      throw new FullException();
    }
    items[tail % items.length]= x;
    tail++;
  }
  
  public T deq() throws EmptyException{
    if (tail - head ==0) {
      throw new EmptyException();
    }
    T x = items[head % items.length];
    head++;
    return x;
  }
  
  
  public Boolean empty() {
    return tail == head;
  }
  

}
