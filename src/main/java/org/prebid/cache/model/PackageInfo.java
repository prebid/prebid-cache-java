package org.prebid.cache.model;

import lombok.Value;
import org.prebid.cache.PBCacheApplication;

@Value
public class PackageInfo {
    String title = PBCacheApplication.class.getPackage().getImplementationTitle();
    String version = PBCacheApplication.class.getPackage().getImplementationVersion();
    String vendorId = PBCacheApplication.class.getPackage().getImplementationVendor();
}
