/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public final class SecurityCfg implements ConfigurationEntry {
  private static final boolean DEFAULT_ENABLED = false;

  private boolean enabled = DEFAULT_ENABLED;
  private File certificateChainPath;
  private File privateKeyPath;
  private final Pkcs12Cfg pkcs12 = new Pkcs12Cfg();

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    final var brokerBasePath = Path.of(brokerBase);
    if (certificateChainPath != null) {
      certificateChainPath = brokerBasePath.resolve(certificateChainPath.toPath()).toFile();
    }

    if (privateKeyPath != null) {
      privateKeyPath = brokerBasePath.resolve(privateKeyPath.toPath()).toFile();
    }

    pkcs12.init(globalConfig, brokerBase);

    if ((certificateChainPath != null || privateKeyPath != null) && pkcs12.getFilePath() != null) {
      throw new IllegalArgumentException(
          String.format(
              """
                      Cannot provide both separate certificate chain and or private key along with a
                      PKCS12 file, use only one approach.

                      certificateChainPath: %s
                      privateKeyPath: %s

                      OR

                      pkcs12Path: %s""",
              certificateChainPath, privateKeyPath, pkcs12.getFilePath()));
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public SecurityCfg setEnabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public File getCertificateChainPath() {
    return certificateChainPath;
  }

  public SecurityCfg setCertificateChainPath(final File certificateChainPath) {
    this.certificateChainPath = certificateChainPath;
    return this;
  }

  public File getPrivateKeyPath() {
    return privateKeyPath;
  }

  public SecurityCfg setPrivateKeyPath(final File privateKeyPath) {
    this.privateKeyPath = privateKeyPath;
    return this;
  }

  public Pkcs12Cfg getPkcs12() {
    return pkcs12;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, certificateChainPath, privateKeyPath);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final var that = (SecurityCfg) o;
    return enabled == that.enabled
        && Objects.equals(certificateChainPath, that.certificateChainPath)
        && Objects.equals(privateKeyPath, that.privateKeyPath);
  }

  @Override
  public String toString() {
    return "SecurityCfg{"
        + "enabled="
        + enabled
        + ", certificateChainPath='"
        + certificateChainPath
        + "'"
        + ", privateKeyPath='"
        + privateKeyPath
        + "'}";
  }
}
