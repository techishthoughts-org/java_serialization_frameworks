/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package org.techishthoughts.avro.model;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class OrderItem extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 2546216890583820275L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"OrderItem\",\"namespace\":\"org.techishthoughts.avro.model\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"productId\",\"type\":\"long\"},{\"name\":\"productName\",\"type\":\"string\"},{\"name\":\"productSku\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"unitPrice\",\"type\":\"string\"},{\"name\":\"totalPrice\",\"type\":\"string\"},{\"name\":\"discount\",\"type\":\"string\"},{\"name\":\"attributes\",\"type\":{\"type\":\"map\",\"values\":\"string\"}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<OrderItem> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<OrderItem> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<OrderItem> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<OrderItem> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<OrderItem> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this OrderItem to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a OrderItem from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a OrderItem instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static OrderItem fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  private long id;
  private long productId;
  private java.lang.CharSequence productName;
  private java.lang.CharSequence productSku;
  private int quantity;
  private java.lang.CharSequence unitPrice;
  private java.lang.CharSequence totalPrice;
  private java.lang.CharSequence discount;
  private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> attributes;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public OrderItem() {}

  /**
   * All-args constructor.
   * @param id The new value for id
   * @param productId The new value for productId
   * @param productName The new value for productName
   * @param productSku The new value for productSku
   * @param quantity The new value for quantity
   * @param unitPrice The new value for unitPrice
   * @param totalPrice The new value for totalPrice
   * @param discount The new value for discount
   * @param attributes The new value for attributes
   */
  public OrderItem(java.lang.Long id, java.lang.Long productId, java.lang.CharSequence productName, java.lang.CharSequence productSku, java.lang.Integer quantity, java.lang.CharSequence unitPrice, java.lang.CharSequence totalPrice, java.lang.CharSequence discount, java.util.Map<java.lang.CharSequence,java.lang.CharSequence> attributes) {
    this.id = id;
    this.productId = productId;
    this.productName = productName;
    this.productSku = productSku;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalPrice = totalPrice;
    this.discount = discount;
    this.attributes = attributes;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return productId;
    case 2: return productName;
    case 3: return productSku;
    case 4: return quantity;
    case 5: return unitPrice;
    case 6: return totalPrice;
    case 7: return discount;
    case 8: return attributes;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.lang.Long)value$; break;
    case 1: productId = (java.lang.Long)value$; break;
    case 2: productName = (java.lang.CharSequence)value$; break;
    case 3: productSku = (java.lang.CharSequence)value$; break;
    case 4: quantity = (java.lang.Integer)value$; break;
    case 5: unitPrice = (java.lang.CharSequence)value$; break;
    case 6: totalPrice = (java.lang.CharSequence)value$; break;
    case 7: discount = (java.lang.CharSequence)value$; break;
    case 8: attributes = (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'id' field.
   * @return The value of the 'id' field.
   */
  public long getId() {
    return id;
  }


  /**
   * Sets the value of the 'id' field.
   * @param value the value to set.
   */
  public void setId(long value) {
    this.id = value;
  }

  /**
   * Gets the value of the 'productId' field.
   * @return The value of the 'productId' field.
   */
  public long getProductId() {
    return productId;
  }


  /**
   * Sets the value of the 'productId' field.
   * @param value the value to set.
   */
  public void setProductId(long value) {
    this.productId = value;
  }

  /**
   * Gets the value of the 'productName' field.
   * @return The value of the 'productName' field.
   */
  public java.lang.CharSequence getProductName() {
    return productName;
  }


  /**
   * Sets the value of the 'productName' field.
   * @param value the value to set.
   */
  public void setProductName(java.lang.CharSequence value) {
    this.productName = value;
  }

  /**
   * Gets the value of the 'productSku' field.
   * @return The value of the 'productSku' field.
   */
  public java.lang.CharSequence getProductSku() {
    return productSku;
  }


  /**
   * Sets the value of the 'productSku' field.
   * @param value the value to set.
   */
  public void setProductSku(java.lang.CharSequence value) {
    this.productSku = value;
  }

  /**
   * Gets the value of the 'quantity' field.
   * @return The value of the 'quantity' field.
   */
  public int getQuantity() {
    return quantity;
  }


  /**
   * Sets the value of the 'quantity' field.
   * @param value the value to set.
   */
  public void setQuantity(int value) {
    this.quantity = value;
  }

  /**
   * Gets the value of the 'unitPrice' field.
   * @return The value of the 'unitPrice' field.
   */
  public java.lang.CharSequence getUnitPrice() {
    return unitPrice;
  }


  /**
   * Sets the value of the 'unitPrice' field.
   * @param value the value to set.
   */
  public void setUnitPrice(java.lang.CharSequence value) {
    this.unitPrice = value;
  }

  /**
   * Gets the value of the 'totalPrice' field.
   * @return The value of the 'totalPrice' field.
   */
  public java.lang.CharSequence getTotalPrice() {
    return totalPrice;
  }


  /**
   * Sets the value of the 'totalPrice' field.
   * @param value the value to set.
   */
  public void setTotalPrice(java.lang.CharSequence value) {
    this.totalPrice = value;
  }

  /**
   * Gets the value of the 'discount' field.
   * @return The value of the 'discount' field.
   */
  public java.lang.CharSequence getDiscount() {
    return discount;
  }


  /**
   * Sets the value of the 'discount' field.
   * @param value the value to set.
   */
  public void setDiscount(java.lang.CharSequence value) {
    this.discount = value;
  }

  /**
   * Gets the value of the 'attributes' field.
   * @return The value of the 'attributes' field.
   */
  public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getAttributes() {
    return attributes;
  }


  /**
   * Sets the value of the 'attributes' field.
   * @param value the value to set.
   */
  public void setAttributes(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
    this.attributes = value;
  }

  /**
   * Creates a new OrderItem RecordBuilder.
   * @return A new OrderItem RecordBuilder
   */
  public static org.techishthoughts.avro.model.OrderItem.Builder newBuilder() {
    return new org.techishthoughts.avro.model.OrderItem.Builder();
  }

  /**
   * Creates a new OrderItem RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new OrderItem RecordBuilder
   */
  public static org.techishthoughts.avro.model.OrderItem.Builder newBuilder(org.techishthoughts.avro.model.OrderItem.Builder other) {
    if (other == null) {
      return new org.techishthoughts.avro.model.OrderItem.Builder();
    } else {
      return new org.techishthoughts.avro.model.OrderItem.Builder(other);
    }
  }

  /**
   * Creates a new OrderItem RecordBuilder by copying an existing OrderItem instance.
   * @param other The existing instance to copy.
   * @return A new OrderItem RecordBuilder
   */
  public static org.techishthoughts.avro.model.OrderItem.Builder newBuilder(org.techishthoughts.avro.model.OrderItem other) {
    if (other == null) {
      return new org.techishthoughts.avro.model.OrderItem.Builder();
    } else {
      return new org.techishthoughts.avro.model.OrderItem.Builder(other);
    }
  }

  /**
   * RecordBuilder for OrderItem instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<OrderItem>
    implements org.apache.avro.data.RecordBuilder<OrderItem> {

    private long id;
    private long productId;
    private java.lang.CharSequence productName;
    private java.lang.CharSequence productSku;
    private int quantity;
    private java.lang.CharSequence unitPrice;
    private java.lang.CharSequence totalPrice;
    private java.lang.CharSequence discount;
    private java.util.Map<java.lang.CharSequence,java.lang.CharSequence> attributes;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(org.techishthoughts.avro.model.OrderItem.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.id)) {
        this.id = data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.productId)) {
        this.productId = data().deepCopy(fields()[1].schema(), other.productId);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.productName)) {
        this.productName = data().deepCopy(fields()[2].schema(), other.productName);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.productSku)) {
        this.productSku = data().deepCopy(fields()[3].schema(), other.productSku);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.quantity)) {
        this.quantity = data().deepCopy(fields()[4].schema(), other.quantity);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
      if (isValidValue(fields()[5], other.unitPrice)) {
        this.unitPrice = data().deepCopy(fields()[5].schema(), other.unitPrice);
        fieldSetFlags()[5] = other.fieldSetFlags()[5];
      }
      if (isValidValue(fields()[6], other.totalPrice)) {
        this.totalPrice = data().deepCopy(fields()[6].schema(), other.totalPrice);
        fieldSetFlags()[6] = other.fieldSetFlags()[6];
      }
      if (isValidValue(fields()[7], other.discount)) {
        this.discount = data().deepCopy(fields()[7].schema(), other.discount);
        fieldSetFlags()[7] = other.fieldSetFlags()[7];
      }
      if (isValidValue(fields()[8], other.attributes)) {
        this.attributes = data().deepCopy(fields()[8].schema(), other.attributes);
        fieldSetFlags()[8] = other.fieldSetFlags()[8];
      }
    }

    /**
     * Creates a Builder by copying an existing OrderItem instance
     * @param other The existing instance to copy.
     */
    private Builder(org.techishthoughts.avro.model.OrderItem other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.productId)) {
        this.productId = data().deepCopy(fields()[1].schema(), other.productId);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.productName)) {
        this.productName = data().deepCopy(fields()[2].schema(), other.productName);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.productSku)) {
        this.productSku = data().deepCopy(fields()[3].schema(), other.productSku);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.quantity)) {
        this.quantity = data().deepCopy(fields()[4].schema(), other.quantity);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.unitPrice)) {
        this.unitPrice = data().deepCopy(fields()[5].schema(), other.unitPrice);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.totalPrice)) {
        this.totalPrice = data().deepCopy(fields()[6].schema(), other.totalPrice);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.discount)) {
        this.discount = data().deepCopy(fields()[7].schema(), other.discount);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.attributes)) {
        this.attributes = data().deepCopy(fields()[8].schema(), other.attributes);
        fieldSetFlags()[8] = true;
      }
    }

    /**
      * Gets the value of the 'id' field.
      * @return The value.
      */
    public long getId() {
      return id;
    }


    /**
      * Sets the value of the 'id' field.
      * @param value The value of 'id'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setId(long value) {
      validate(fields()[0], value);
      this.id = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'id' field has been set.
      * @return True if the 'id' field has been set, false otherwise.
      */
    public boolean hasId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'id' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearId() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'productId' field.
      * @return The value.
      */
    public long getProductId() {
      return productId;
    }


    /**
      * Sets the value of the 'productId' field.
      * @param value The value of 'productId'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setProductId(long value) {
      validate(fields()[1], value);
      this.productId = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'productId' field has been set.
      * @return True if the 'productId' field has been set, false otherwise.
      */
    public boolean hasProductId() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'productId' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearProductId() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'productName' field.
      * @return The value.
      */
    public java.lang.CharSequence getProductName() {
      return productName;
    }


    /**
      * Sets the value of the 'productName' field.
      * @param value The value of 'productName'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setProductName(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.productName = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'productName' field has been set.
      * @return True if the 'productName' field has been set, false otherwise.
      */
    public boolean hasProductName() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'productName' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearProductName() {
      productName = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'productSku' field.
      * @return The value.
      */
    public java.lang.CharSequence getProductSku() {
      return productSku;
    }


    /**
      * Sets the value of the 'productSku' field.
      * @param value The value of 'productSku'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setProductSku(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.productSku = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'productSku' field has been set.
      * @return True if the 'productSku' field has been set, false otherwise.
      */
    public boolean hasProductSku() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'productSku' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearProductSku() {
      productSku = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'quantity' field.
      * @return The value.
      */
    public int getQuantity() {
      return quantity;
    }


    /**
      * Sets the value of the 'quantity' field.
      * @param value The value of 'quantity'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setQuantity(int value) {
      validate(fields()[4], value);
      this.quantity = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'quantity' field has been set.
      * @return True if the 'quantity' field has been set, false otherwise.
      */
    public boolean hasQuantity() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'quantity' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearQuantity() {
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'unitPrice' field.
      * @return The value.
      */
    public java.lang.CharSequence getUnitPrice() {
      return unitPrice;
    }


    /**
      * Sets the value of the 'unitPrice' field.
      * @param value The value of 'unitPrice'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setUnitPrice(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.unitPrice = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'unitPrice' field has been set.
      * @return True if the 'unitPrice' field has been set, false otherwise.
      */
    public boolean hasUnitPrice() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'unitPrice' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearUnitPrice() {
      unitPrice = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'totalPrice' field.
      * @return The value.
      */
    public java.lang.CharSequence getTotalPrice() {
      return totalPrice;
    }


    /**
      * Sets the value of the 'totalPrice' field.
      * @param value The value of 'totalPrice'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setTotalPrice(java.lang.CharSequence value) {
      validate(fields()[6], value);
      this.totalPrice = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'totalPrice' field has been set.
      * @return True if the 'totalPrice' field has been set, false otherwise.
      */
    public boolean hasTotalPrice() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'totalPrice' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearTotalPrice() {
      totalPrice = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /**
      * Gets the value of the 'discount' field.
      * @return The value.
      */
    public java.lang.CharSequence getDiscount() {
      return discount;
    }


    /**
      * Sets the value of the 'discount' field.
      * @param value The value of 'discount'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setDiscount(java.lang.CharSequence value) {
      validate(fields()[7], value);
      this.discount = value;
      fieldSetFlags()[7] = true;
      return this;
    }

    /**
      * Checks whether the 'discount' field has been set.
      * @return True if the 'discount' field has been set, false otherwise.
      */
    public boolean hasDiscount() {
      return fieldSetFlags()[7];
    }


    /**
      * Clears the value of the 'discount' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearDiscount() {
      discount = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /**
      * Gets the value of the 'attributes' field.
      * @return The value.
      */
    public java.util.Map<java.lang.CharSequence,java.lang.CharSequence> getAttributes() {
      return attributes;
    }


    /**
      * Sets the value of the 'attributes' field.
      * @param value The value of 'attributes'.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder setAttributes(java.util.Map<java.lang.CharSequence,java.lang.CharSequence> value) {
      validate(fields()[8], value);
      this.attributes = value;
      fieldSetFlags()[8] = true;
      return this;
    }

    /**
      * Checks whether the 'attributes' field has been set.
      * @return True if the 'attributes' field has been set, false otherwise.
      */
    public boolean hasAttributes() {
      return fieldSetFlags()[8];
    }


    /**
      * Clears the value of the 'attributes' field.
      * @return This builder.
      */
    public org.techishthoughts.avro.model.OrderItem.Builder clearAttributes() {
      attributes = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OrderItem build() {
      try {
        OrderItem record = new OrderItem();
        record.id = fieldSetFlags()[0] ? this.id : (java.lang.Long) defaultValue(fields()[0]);
        record.productId = fieldSetFlags()[1] ? this.productId : (java.lang.Long) defaultValue(fields()[1]);
        record.productName = fieldSetFlags()[2] ? this.productName : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.productSku = fieldSetFlags()[3] ? this.productSku : (java.lang.CharSequence) defaultValue(fields()[3]);
        record.quantity = fieldSetFlags()[4] ? this.quantity : (java.lang.Integer) defaultValue(fields()[4]);
        record.unitPrice = fieldSetFlags()[5] ? this.unitPrice : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.totalPrice = fieldSetFlags()[6] ? this.totalPrice : (java.lang.CharSequence) defaultValue(fields()[6]);
        record.discount = fieldSetFlags()[7] ? this.discount : (java.lang.CharSequence) defaultValue(fields()[7]);
        record.attributes = fieldSetFlags()[8] ? this.attributes : (java.util.Map<java.lang.CharSequence,java.lang.CharSequence>) defaultValue(fields()[8]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<OrderItem>
    WRITER$ = (org.apache.avro.io.DatumWriter<OrderItem>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<OrderItem>
    READER$ = (org.apache.avro.io.DatumReader<OrderItem>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeLong(this.id);

    out.writeLong(this.productId);

    out.writeString(this.productName);

    out.writeString(this.productSku);

    out.writeInt(this.quantity);

    out.writeString(this.unitPrice);

    out.writeString(this.totalPrice);

    out.writeString(this.discount);

    long size0 = this.attributes.size();
    out.writeMapStart();
    out.setItemCount(size0);
    long actualSize0 = 0;
    for (java.util.Map.Entry<java.lang.CharSequence, java.lang.CharSequence> e0: this.attributes.entrySet()) {
      actualSize0++;
      out.startItem();
      out.writeString(e0.getKey());
      java.lang.CharSequence v0 = e0.getValue();
      out.writeString(v0);
    }
    out.writeMapEnd();
    if (actualSize0 != size0)
      throw new java.util.ConcurrentModificationException("Map-size written was " + size0 + ", but element count was " + actualSize0 + ".");

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.id = in.readLong();

      this.productId = in.readLong();

      this.productName = in.readString(this.productName instanceof Utf8 ? (Utf8)this.productName : null);

      this.productSku = in.readString(this.productSku instanceof Utf8 ? (Utf8)this.productSku : null);

      this.quantity = in.readInt();

      this.unitPrice = in.readString(this.unitPrice instanceof Utf8 ? (Utf8)this.unitPrice : null);

      this.totalPrice = in.readString(this.totalPrice instanceof Utf8 ? (Utf8)this.totalPrice : null);

      this.discount = in.readString(this.discount instanceof Utf8 ? (Utf8)this.discount : null);

      long size0 = in.readMapStart();
      java.util.Map<java.lang.CharSequence,java.lang.CharSequence> m0 = this.attributes; // Need fresh name due to limitation of macro system
      if (m0 == null) {
        m0 = new java.util.HashMap<java.lang.CharSequence,java.lang.CharSequence>((int)size0);
        this.attributes = m0;
      } else m0.clear();
      for ( ; 0 < size0; size0 = in.mapNext()) {
        for ( ; size0 != 0; size0--) {
          java.lang.CharSequence k0 = null;
          k0 = in.readString(k0 instanceof Utf8 ? (Utf8)k0 : null);
          java.lang.CharSequence v0 = null;
          v0 = in.readString(v0 instanceof Utf8 ? (Utf8)v0 : null);
          m0.put(k0, v0);
        }
      }

    } else {
      for (int i = 0; i < 9; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.id = in.readLong();
          break;

        case 1:
          this.productId = in.readLong();
          break;

        case 2:
          this.productName = in.readString(this.productName instanceof Utf8 ? (Utf8)this.productName : null);
          break;

        case 3:
          this.productSku = in.readString(this.productSku instanceof Utf8 ? (Utf8)this.productSku : null);
          break;

        case 4:
          this.quantity = in.readInt();
          break;

        case 5:
          this.unitPrice = in.readString(this.unitPrice instanceof Utf8 ? (Utf8)this.unitPrice : null);
          break;

        case 6:
          this.totalPrice = in.readString(this.totalPrice instanceof Utf8 ? (Utf8)this.totalPrice : null);
          break;

        case 7:
          this.discount = in.readString(this.discount instanceof Utf8 ? (Utf8)this.discount : null);
          break;

        case 8:
          long size0 = in.readMapStart();
          java.util.Map<java.lang.CharSequence,java.lang.CharSequence> m0 = this.attributes; // Need fresh name due to limitation of macro system
          if (m0 == null) {
            m0 = new java.util.HashMap<java.lang.CharSequence,java.lang.CharSequence>((int)size0);
            this.attributes = m0;
          } else m0.clear();
          for ( ; 0 < size0; size0 = in.mapNext()) {
            for ( ; size0 != 0; size0--) {
              java.lang.CharSequence k0 = null;
              k0 = in.readString(k0 instanceof Utf8 ? (Utf8)k0 : null);
              java.lang.CharSequence v0 = null;
              v0 = in.readString(v0 instanceof Utf8 ? (Utf8)v0 : null);
              m0.put(k0, v0);
            }
          }
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










