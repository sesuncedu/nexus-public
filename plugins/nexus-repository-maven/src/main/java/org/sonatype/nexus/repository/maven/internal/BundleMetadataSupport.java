package org.sonatype.nexus.repository.maven.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.maven.index.reader.Record;
import org.apache.maven.index.reader.RecordCompactor;
import org.apache.maven.index.reader.RecordExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides support for extracting osgi headers from maven bundles, and for adding
 * these fields to the exported indexer. Field names are taken from the fork of maven.index.reader
 * submitted as Pull Request #13 - see: https://github.com/apache/maven-indexer/pull/13
 *
 * That package is an osgi minor bump from 5.1.2 due to added fields for PROVIDE/REQUIRE CAPABILITY,
 * FRAGMENT_HOST, and BREE, and sha256 checksum
 */
public class BundleMetadataSupport {
    public static final String OSGI_ROOT = "osgi";
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(BundleMetadataSupport.class);
     static final Record.EntryKey<String> RECORD_SHA_256 = new Record.EntryKey<>("sha256",String.class);

    private static final Map<String,String> headerToAttrName = new HashMap<>();
    private static final Map<String,Record.EntryKey<String>> attrToEntryKey = new HashMap<>();
    private static final Map<String,Record.EntryKey<String>> headerToField = new HashMap<>();
     static final String OSGI_SELECT_FIELDS;

    private static void addHeaderMapping(String header, String attrName) {
        headerToAttrName.put(header,attrName);
        attrToEntryKey.put(attrName,new Record.EntryKey<>(header,String.class));
    }

    static {
         addHeaderMapping("Bundle-SymbolicName","bsn");
         addHeaderMapping(   "Bundle-Version","bv");
         addHeaderMapping(   "Export-Package","exports");
         addHeaderMapping(   "Export-Service","exportService");
         addHeaderMapping(   "Bundle-Description","description");
         addHeaderMapping(   "Bundle-Name","name");
         addHeaderMapping(   "Bundle-License","license");
         addHeaderMapping(   "Bundle-DocURL","docUrl");
         addHeaderMapping(   "Require-Bundle","reqBundle");
         addHeaderMapping(   "Provide-Capability","provides");
         addHeaderMapping(   "Require-Capability","requires");
         addHeaderMapping(   "Fragment-Host","fragHost");
         addHeaderMapping(   "Bundle-RequiredExecutionEnvironment","bree");

        addHeaderToFieldMapping("Provide-Capability");
        addHeaderToFieldMapping("Require-Capability");
        addHeaderToFieldMapping("Fragment-Host");
        addHeaderToFieldMapping("Bundle-RequiredExecutionEnvironment");
         OSGI_SELECT_FIELDS = osgiSelectFields();
    }

    private static void addHeaderToFieldMapping(String header) {
        headerToField.put(header,new Record.EntryKey<>(header,String.class));
    }

    static void extractOsgiAttributes(Asset asset, AssetBlob assetBlob) {
        try (JarInputStream in = new JarInputStream(assetBlob.getBlob().getInputStream())) {
            Manifest manifest = in.getManifest();
            java.util.jar.Attributes mainAttributes = manifest.getMainAttributes();
            if(mainAttributes != null && mainAttributes.get("Bundle-SymbolicName") != null) {
                NestedAttributesMap attributesMap = asset.attributes().child(OSGI_ROOT);
                for (Map.Entry<String, String> entry : headerToAttrName.entrySet()) {
                    String headerName = entry.getKey();
                    String attrName = entry.getValue();
                    String value = mainAttributes.getValue(headerName);
                    if(value != null) {
                        attributesMap.set(attrName,value);
                    }
                }
            }
        }  catch(IOException e) {
            logger.debug("failed to extract osgi attributes for asset",e);
        }
    }

    static void addOsgiFieldsToRecord(Record record, ODocument document) {
        for (Map.Entry<String, Record.EntryKey<String>> entry : attrToEntryKey.entrySet()) {
            String attrName = entry.getKey();
            Record.EntryKey<String> field = entry.getValue();
            if(document.containsField(attrName)) {
                record.put(field,document.field(attrName,String.class));
            }
        }
    }

    static class EnhancedOsgiRecordExpander extends RecordExpander {
        @Override
        public Record apply(Map<String, String> recordMap) {
            Record record = super.apply(recordMap);
            String s = recordMap.get("sha256");
            if(s != null) {
                record.put(RECORD_SHA_256,s);
            }
            for (Map.Entry<String, Record.EntryKey<String>> entry : headerToField.entrySet()) {
                String header = entry.getKey();
                Record.EntryKey<String> field = entry.getValue();
                s = recordMap.get(header);
                if(s != null) {
                    record.put(field,s);
                }
            }
            return record;
        }
    }

    static class EnhancedOsgiRecordCompactor extends RecordCompactor {
        @Override
        public Map<String, String> apply(Record record) {
            Map<String, String> map = super.apply(record);
            String s = record.get(RECORD_SHA_256);
            if(s != null) {
                map.put("sha256",s);
            }
            for (Map.Entry<String, Record.EntryKey<String>> entry : headerToField.entrySet()) {
                String header = entry.getKey();
                Record.EntryKey<String> field = entry.getValue();
                s = record.get(field);
                if(s != null) {
                    map.put(header,s);
                }
            }

            return map;
        }
    }
    private static String osgiSelectFields() {
        StringBuilder buf = new StringBuilder();
        for (Iterator<String> iterator = attrToEntryKey.keySet().iterator(); iterator.hasNext(); ) {
            String attrName = iterator.next();
            buf.append("attributes.");
            buf.append(OSGI_ROOT);
            buf.append(".");
            buf.append(attrName);
            buf.append(" as ");
            buf.append(attrName);
            if(iterator.hasNext()) {
                buf.append(", ");
            }
        }
       return buf.toString();
    }

}
