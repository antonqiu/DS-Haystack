package com.cmu.ds.haystacks.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HSConfig {

  @JsonProperty("object_store_addresses")
  private String[] objectStoreAddresses;
  @JsonProperty("object_store_port")
  private int objectStorePort;
  @JsonProperty("object_cache_addresses")
  private String[] objectCacheAddresses;

  public String[] getObjectStoreAddresses() {
    return objectStoreAddresses;
  }

  public void setObjectStoreAddresses(String[] objectStoreAddresses) {
    this.objectStoreAddresses = objectStoreAddresses;
  }

  public int getObjectStorePort() {
    return objectStorePort;
  }

  public void setObjectStorePort(int objectStorePort) {
    this.objectStorePort = objectStorePort;
  }

  public String[] getObjectCacheAddresses() {
    return objectCacheAddresses;
  }

  public void setObjectCacheAddresses(String[] objectCacheAddresses) {
    this.objectCacheAddresses = objectCacheAddresses;
  }
}
