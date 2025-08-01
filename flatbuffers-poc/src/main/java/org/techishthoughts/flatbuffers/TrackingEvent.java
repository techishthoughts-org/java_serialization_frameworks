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
public final class TrackingEvent extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_25_2_10(); }
  public static TrackingEvent getRootAsTrackingEvent(ByteBuffer _bb) { return getRootAsTrackingEvent(_bb, new TrackingEvent()); }
  public static TrackingEvent getRootAsTrackingEvent(ByteBuffer _bb, TrackingEvent obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public TrackingEvent __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public long id() { int o = __offset(4); return o != 0 ? bb.getLong(o + bb_pos) : 0L; }
  public boolean mutateId(long id) { int o = __offset(4); if (o != 0) { bb.putLong(o + bb_pos, id); return true; } else { return false; } }
  public String eventType() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer eventTypeAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public ByteBuffer eventTypeInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 1); }
  public String timestamp() { int o = __offset(8); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer timestampAsByteBuffer() { return __vector_as_bytebuffer(8, 1); }
  public ByteBuffer timestampInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 8, 1); }
  public String location() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer locationAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer locationInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }
  public String description() { int o = __offset(12); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer descriptionAsByteBuffer() { return __vector_as_bytebuffer(12, 1); }
  public ByteBuffer descriptionInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 12, 1); }
  public String status() { int o = __offset(14); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer statusAsByteBuffer() { return __vector_as_bytebuffer(14, 1); }
  public ByteBuffer statusInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 14, 1); }

  public static int createTrackingEvent(FlatBufferBuilder builder,
      long id,
      int eventTypeOffset,
      int timestampOffset,
      int locationOffset,
      int descriptionOffset,
      int statusOffset) {
    builder.startTable(6);
    TrackingEvent.addId(builder, id);
    TrackingEvent.addStatus(builder, statusOffset);
    TrackingEvent.addDescription(builder, descriptionOffset);
    TrackingEvent.addLocation(builder, locationOffset);
    TrackingEvent.addTimestamp(builder, timestampOffset);
    TrackingEvent.addEventType(builder, eventTypeOffset);
    return TrackingEvent.endTrackingEvent(builder);
  }

  public static void startTrackingEvent(FlatBufferBuilder builder) { builder.startTable(6); }
  public static void addId(FlatBufferBuilder builder, long id) { builder.addLong(0, id, 0L); }
  public static void addEventType(FlatBufferBuilder builder, int eventTypeOffset) { builder.addOffset(1, eventTypeOffset, 0); }
  public static void addTimestamp(FlatBufferBuilder builder, int timestampOffset) { builder.addOffset(2, timestampOffset, 0); }
  public static void addLocation(FlatBufferBuilder builder, int locationOffset) { builder.addOffset(3, locationOffset, 0); }
  public static void addDescription(FlatBufferBuilder builder, int descriptionOffset) { builder.addOffset(4, descriptionOffset, 0); }
  public static void addStatus(FlatBufferBuilder builder, int statusOffset) { builder.addOffset(5, statusOffset, 0); }
  public static int endTrackingEvent(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public TrackingEvent get(int j) { return get(new TrackingEvent(), j); }
    public TrackingEvent get(TrackingEvent obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

