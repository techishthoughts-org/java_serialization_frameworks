// automatically generated by the FlatBuffers compiler, do not modify

package org.techishthoughts.flatbuffers;

@SuppressWarnings("unused")
public final class PaymentStatus {
  private PaymentStatus() { }
  public static final byte PENDING = 0;
  public static final byte PROCESSING = 1;
  public static final byte COMPLETED = 2;
  public static final byte FAILED = 3;
  public static final byte REFUNDED = 4;

  public static final String[] names = { "PENDING", "PROCESSING", "COMPLETED", "FAILED", "REFUNDED", };

  public static String name(int e) { return names[e]; }
}

