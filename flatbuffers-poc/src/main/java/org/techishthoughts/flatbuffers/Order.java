// automatically generated by the FlatBuffers compiler, do not modify

package org.techishthoughts.flatbuffers;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.BooleanVector;
import com.google.flatbuffers.ByteVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.DoubleVector;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.FloatVector;
import com.google.flatbuffers.IntVector;
import com.google.flatbuffers.LongVector;
import com.google.flatbuffers.ShortVector;
import com.google.flatbuffers.StringVector;
import com.google.flatbuffers.Struct;
import com.google.flatbuffers.Table;
import com.google.flatbuffers.UnionVector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class Order extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_25_2_10(); }
  public static Order getRootAsOrder(ByteBuffer _bb) { return getRootAsOrder(_bb, new Order()); }
  public static Order getRootAsOrder(ByteBuffer _bb, Order obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public Order __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public long id() { int o = __offset(4); return o != 0 ? bb.getLong(o + bb_pos) : 0L; }
  public boolean mutateId(long id) { int o = __offset(4); if (o != 0) { bb.putLong(o + bb_pos, id); return true; } else { return false; } }
  public String orderNumber() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer orderNumberAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public ByteBuffer orderNumberInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 1); }
  public String orderDate() { int o = __offset(8); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer orderDateAsByteBuffer() { return __vector_as_bytebuffer(8, 1); }
  public ByteBuffer orderDateInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 8, 1); }
  public byte status() { int o = __offset(10); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public boolean mutateStatus(byte status) { int o = __offset(10); if (o != 0) { bb.put(o + bb_pos, status); return true; } else { return false; } }
  public double totalAmount() { int o = __offset(12); return o != 0 ? bb.getDouble(o + bb_pos) : 0.0; }
  public boolean mutateTotalAmount(double totalAmount) { int o = __offset(12); if (o != 0) { bb.putDouble(o + bb_pos, totalAmount); return true; } else { return false; } }
  public String currency() { int o = __offset(14); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer currencyAsByteBuffer() { return __vector_as_bytebuffer(14, 1); }
  public ByteBuffer currencyInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 14, 1); }
  public org.techishthoughts.flatbuffers.OrderItem items(int j) { return items(new org.techishthoughts.flatbuffers.OrderItem(), j); }
  public org.techishthoughts.flatbuffers.OrderItem items(org.techishthoughts.flatbuffers.OrderItem obj, int j) { int o = __offset(16); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int itemsLength() { int o = __offset(16); return o != 0 ? __vector_len(o) : 0; }
  public org.techishthoughts.flatbuffers.OrderItem.Vector itemsVector() { return itemsVector(new org.techishthoughts.flatbuffers.OrderItem.Vector()); }
  public org.techishthoughts.flatbuffers.OrderItem.Vector itemsVector(org.techishthoughts.flatbuffers.OrderItem.Vector obj) { int o = __offset(16); return o != 0 ? obj.__assign(__vector(o), 4, bb) : null; }
  public org.techishthoughts.flatbuffers.Payment payment() { return payment(new org.techishthoughts.flatbuffers.Payment()); }
  public org.techishthoughts.flatbuffers.Payment payment(org.techishthoughts.flatbuffers.Payment obj) { int o = __offset(18); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public org.techishthoughts.flatbuffers.Address shippingAddress() { return shippingAddress(new org.techishthoughts.flatbuffers.Address()); }
  public org.techishthoughts.flatbuffers.Address shippingAddress(org.techishthoughts.flatbuffers.Address obj) { int o = __offset(20); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public org.techishthoughts.flatbuffers.Address billingAddress() { return billingAddress(new org.techishthoughts.flatbuffers.Address()); }
  public org.techishthoughts.flatbuffers.Address billingAddress(org.techishthoughts.flatbuffers.Address obj) { int o = __offset(22); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createOrder(FlatBufferBuilder builder,
      long id,
      int orderNumberOffset,
      int orderDateOffset,
      byte status,
      double totalAmount,
      int currencyOffset,
      int itemsOffset,
      int paymentOffset,
      int shippingAddressOffset,
      int billingAddressOffset) {
    builder.startTable(10);
    Order.addTotalAmount(builder, totalAmount);
    Order.addId(builder, id);
    Order.addBillingAddress(builder, billingAddressOffset);
    Order.addShippingAddress(builder, shippingAddressOffset);
    Order.addPayment(builder, paymentOffset);
    Order.addItems(builder, itemsOffset);
    Order.addCurrency(builder, currencyOffset);
    Order.addOrderDate(builder, orderDateOffset);
    Order.addOrderNumber(builder, orderNumberOffset);
    Order.addStatus(builder, status);
    return Order.endOrder(builder);
  }

  public static void startOrder(FlatBufferBuilder builder) { builder.startTable(10); }
  public static void addId(FlatBufferBuilder builder, long id) { builder.addLong(0, id, 0L); }
  public static void addOrderNumber(FlatBufferBuilder builder, int orderNumberOffset) { builder.addOffset(1, orderNumberOffset, 0); }
  public static void addOrderDate(FlatBufferBuilder builder, int orderDateOffset) { builder.addOffset(2, orderDateOffset, 0); }
  public static void addStatus(FlatBufferBuilder builder, byte status) { builder.addByte(3, status, 0); }
  public static void addTotalAmount(FlatBufferBuilder builder, double totalAmount) { builder.addDouble(4, totalAmount, 0.0); }
  public static void addCurrency(FlatBufferBuilder builder, int currencyOffset) { builder.addOffset(5, currencyOffset, 0); }
  public static void addItems(FlatBufferBuilder builder, int itemsOffset) { builder.addOffset(6, itemsOffset, 0); }
  public static int createItemsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startItemsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addPayment(FlatBufferBuilder builder, int paymentOffset) { builder.addOffset(7, paymentOffset, 0); }
  public static void addShippingAddress(FlatBufferBuilder builder, int shippingAddressOffset) { builder.addOffset(8, shippingAddressOffset, 0); }
  public static void addBillingAddress(FlatBufferBuilder builder, int billingAddressOffset) { builder.addOffset(9, billingAddressOffset, 0); }
  public static int endOrder(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public Order get(int j) { return get(new Order(), j); }
    public Order get(Order obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

