package com.cmu.ds.haystacks.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HSConfig {

  @JsonProperty("object_store_addresses")
  private String[] objectStoreAddresses;

  @JsonProperty("object_store_port")
  private int objectStorePort;

  @JsonProperty("object_cache_addresses")
  private String[] objectCacheAddresses;

  @JsonProperty("num_logical_volumes")
  private int numLogicalVolumes;

  @JsonProperty("store_replication_factor")
  private int storeReplicationFactor;

  public int getStoreReplicationFactor() {
    return storeReplicationFactor;
  }

  public void setStoreReplicationFactor(int storeReplicationFactor) {
    this.storeReplicationFactor = storeReplicationFactor;
  }

  public int getNumLogicalVolumes() {
    return numLogicalVolumes;
  }

  public void setNumLogicalVolumes(int numLogicalVolumes) {
    this.numLogicalVolumes = numLogicalVolumes;
  }

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
