package network;

public interface OnRequestCallBack {

    byte[] handleTask(byte[] exchange);

    String getEndPoint();
}
