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
public final class SocialConnection extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_25_2_10(); }
  public static SocialConnection getRootAsSocialConnection(ByteBuffer _bb) { return getRootAsSocialConnection(_bb, new SocialConnection()); }
  public static SocialConnection getRootAsSocialConnection(ByteBuffer _bb, SocialConnection obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public SocialConnection __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public long id() { int o = __offset(4); return o != 0 ? bb.getLong(o + bb_pos) : 0L; }
  public boolean mutateId(long id) { int o = __offset(4); if (o != 0) { bb.putLong(o + bb_pos, id); return true; } else { return false; } }
  public byte platform() { int o = __offset(6); return o != 0 ? bb.get(o + bb_pos) : 0; }
  public boolean mutatePlatform(byte platform) { int o = __offset(6); if (o != 0) { bb.put(o + bb_pos, platform); return true; } else { return false; } }
  public String username() { int o = __offset(8); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer usernameAsByteBuffer() { return __vector_as_bytebuffer(8, 1); }
  public ByteBuffer usernameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 8, 1); }
  public String profileUrl() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer profileUrlAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer profileUrlInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }
  public int followersCount() { int o = __offset(12); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public boolean mutateFollowersCount(int followersCount) { int o = __offset(12); if (o != 0) { bb.putInt(o + bb_pos, followersCount); return true; } else { return false; } }
  public boolean isVerified() { int o = __offset(14); return o != 0 ? 0!=bb.get(o + bb_pos) : false; }
  public boolean mutateIsVerified(boolean isVerified) { int o = __offset(14); if (o != 0) { bb.put(o + bb_pos, (byte)(isVerified ? 1 : 0)); return true; } else { return false; } }
  public String connectedAt() { int o = __offset(16); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer connectedAtAsByteBuffer() { return __vector_as_bytebuffer(16, 1); }
  public ByteBuffer connectedAtInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 16, 1); }

  public static int createSocialConnection(FlatBufferBuilder builder,
      long id,
      byte platform,
      int usernameOffset,
      int profileUrlOffset,
      int followersCount,
      boolean isVerified,
      int connectedAtOffset) {
    builder.startTable(7);
    SocialConnection.addId(builder, id);
    SocialConnection.addConnectedAt(builder, connectedAtOffset);
    SocialConnection.addFollowersCount(builder, followersCount);
    SocialConnection.addProfileUrl(builder, profileUrlOffset);
    SocialConnection.addUsername(builder, usernameOffset);
    SocialConnection.addIsVerified(builder, isVerified);
    SocialConnection.addPlatform(builder, platform);
    return SocialConnection.endSocialConnection(builder);
  }

  public static void startSocialConnection(FlatBufferBuilder builder) { builder.startTable(7); }
  public static void addId(FlatBufferBuilder builder, long id) { builder.addLong(0, id, 0L); }
  public static void addPlatform(FlatBufferBuilder builder, byte platform) { builder.addByte(1, platform, 0); }
  public static void addUsername(FlatBufferBuilder builder, int usernameOffset) { builder.addOffset(2, usernameOffset, 0); }
  public static void addProfileUrl(FlatBufferBuilder builder, int profileUrlOffset) { builder.addOffset(3, profileUrlOffset, 0); }
  public static void addFollowersCount(FlatBufferBuilder builder, int followersCount) { builder.addInt(4, followersCount, 0); }
  public static void addIsVerified(FlatBufferBuilder builder, boolean isVerified) { builder.addBoolean(5, isVerified, false); }
  public static void addConnectedAt(FlatBufferBuilder builder, int connectedAtOffset) { builder.addOffset(6, connectedAtOffset, 0); }
  public static int endSocialConnection(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public SocialConnection get(int j) { return get(new SocialConnection(), j); }
    public SocialConnection get(SocialConnection obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

