/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.writer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.base.BaseAnnotation;
import org.jf.dexlib2.base.BaseAnnotationElement;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31c;
import org.jf.dexlib2.dexbacked.raw.ClassDefItem;
import org.jf.dexlib2.dexbacked.raw.FieldIdItem;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;
import org.jf.dexlib2.dexbacked.raw.ItemType;
import org.jf.dexlib2.dexbacked.raw.MethodIdItem;
import org.jf.dexlib2.dexbacked.raw.ProtoIdItem;
import org.jf.dexlib2.dexbacked.raw.StringIdItem;
import org.jf.dexlib2.dexbacked.raw.TypeIdItem;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.LineNumber;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.VariableRegisterInstruction;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction10x;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction12x;
import org.jf.dexlib2.iface.instruction.formats.Instruction20bc;
import org.jf.dexlib2.iface.instruction.formats.Instruction20t;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21lh;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22b;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22cs;
import org.jf.dexlib2.iface.instruction.formats.Instruction22s;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22x;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction30t;
import org.jf.dexlib2.iface.instruction.formats.Instruction31c;
import org.jf.dexlib2.iface.instruction.formats.Instruction31i;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import org.jf.dexlib2.iface.instruction.formats.Instruction32x;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.instruction.formats.Instruction35mi;
import org.jf.dexlib2.iface.instruction.formats.Instruction35ms;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rmi;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rms;
import org.jf.dexlib2.iface.instruction.formats.Instruction45cc;
import org.jf.dexlib2.iface.instruction.formats.Instruction4rcc;
import org.jf.dexlib2.iface.instruction.formats.Instruction51l;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.SparseSwitchPayload;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.util.InstructionUtil;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.writer.io.DeferredOutputStream;
import org.jf.dexlib2.writer.io.DeferredOutputStreamFactory;
import org.jf.dexlib2.writer.io.DexDataStore;
import org.jf.dexlib2.writer.io.MemoryDeferredOutputStream;
import org.jf.dexlib2.writer.util.TryListBuilder;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Adler32;

public abstract class DexWriter<
        StringKey extends CharSequence, StringRef extends StringReference, TypeKey extends CharSequence,
        TypeRef extends TypeReference, ProtoRefKey extends MethodProtoReference,
        FieldRefKey extends FieldReference, MethodRefKey extends MethodReference,
        ClassKey extends Comparable<? super ClassKey>,
        AnnotationKey extends Annotation, AnnotationSetKey,
        TypeListKey,
        FieldKey, MethodKey,
        EncodedValue,
        AnnotationElement extends org.jf.dexlib2.iface.AnnotationElement,
        StringSectionType extends StringSection<StringKey, StringRef>,
        TypeSectionType extends TypeSection<StringKey, TypeKey, TypeRef>,
        ProtoSectionType extends ProtoSection<StringKey, TypeKey, ProtoRefKey, TypeListKey>,
        FieldSectionType extends FieldSection<StringKey, TypeKey, FieldRefKey, FieldKey>,
        MethodSectionType extends MethodSection<StringKey, TypeKey, ProtoRefKey, MethodRefKey, MethodKey>,
        ClassSectionType extends ClassSection<StringKey, TypeKey, TypeListKey, ClassKey, FieldKey, MethodKey,
                AnnotationSetKey, EncodedValue>,
        TypeListSectionType extends TypeListSection<TypeKey, TypeListKey>,
        AnnotationSectionType extends AnnotationSection<StringKey, TypeKey, AnnotationKey, AnnotationElement,
                EncodedValue>,
        AnnotationSetSectionType extends AnnotationSetSection<AnnotationKey, AnnotationSetKey>> {
    public static final int NO_INDEX = -1;
    public static final int NO_OFFSET = 0;
    private static Comparator<Map.Entry> toStringKeyComparator =
            new Comparator<Map.Entry>() {
                @Override
                public int compare(Entry o1, Entry o2) {
                    return o1.getKey().toString().compareTo(o2.getKey().toString());
                }
            };
    public final StringSectionType stringSection;
    public final TypeSectionType typeSection;
    public final ProtoSectionType protoSection;
    public final FieldSectionType fieldSection;
    public final MethodSectionType methodSection;
    public final ClassSectionType classSection;
    public final TypeListSectionType typeListSection;
    public final AnnotationSectionType annotationSection;
    public final AnnotationSetSectionType annotationSetSection;
    protected final Opcodes opcodes;
    protected int stringIndexSectionOffset = NO_OFFSET;
    protected int typeSectionOffset = NO_OFFSET;
    protected int protoSectionOffset = NO_OFFSET;
    protected int fieldSectionOffset = NO_OFFSET;
    protected int methodSectionOffset = NO_OFFSET;
    protected int classIndexSectionOffset = NO_OFFSET;
    protected int stringDataSectionOffset = NO_OFFSET;
    protected int classDataSectionOffset = NO_OFFSET;
    protected int typeListSectionOffset = NO_OFFSET;
    protected int encodedArraySectionOffset = NO_OFFSET;
    protected int annotationSectionOffset = NO_OFFSET;
    protected int annotationSetSectionOffset = NO_OFFSET;
    protected int annotationSetRefSectionOffset = NO_OFFSET;
    protected int annotationDirectorySectionOffset = NO_OFFSET;
    protected int debugSectionOffset = NO_OFFSET;
    protected int codeSectionOffset = NO_OFFSET;
    protected int mapSectionOffset = NO_OFFSET;
    protected int numEncodedArrayItems = 0;
    protected int numAnnotationSetRefItems = 0;
    protected int numAnnotationDirectoryItems = 0;
    protected int numDebugInfoItems = 0;
    protected int numCodeItemItems = 0;
    protected int numClassDataItems = 0;

    protected DexWriter(Opcodes opcodes) {
        this.opcodes = opcodes;

        SectionProvider sectionProvider = getSectionProvider();
        this.stringSection = sectionProvider.getStringSection();
        this.typeSection = sectionProvider.getTypeSection();
        this.protoSection = sectionProvider.getProtoSection();
        this.fieldSection = sectionProvider.getFieldSection();
        this.methodSection = sectionProvider.getMethodSection();
        this.classSection = sectionProvider.getClassSection();
        this.typeListSection = sectionProvider.getTypeListSection();
        this.annotationSection = sectionProvider.getAnnotationSection();
        this.annotationSetSection = sectionProvider.getAnnotationSetSection();
    }

    private static <T extends Comparable<? super T>> Comparator<Map.Entry<? extends T, ?>> comparableKeyComparator() {
        return new Comparator<Entry<? extends T, ?>>() {
            @Override
            public int compare(Entry<? extends T, ?> o1, Entry<? extends T, ?> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        };
    }

    private static DexDataWriter outputAt(DexDataStore dataStore, int filePosition) throws IOException {
        return new DexDataWriter(dataStore.outputAt(filePosition), filePosition);
    }

    @NonNull
    protected abstract SectionProvider getSectionProvider();

    protected abstract void writeEncodedValue(@NonNull InternalEncodedValueWriter writer,
                                              @NonNull EncodedValue encodedValue) throws IOException;

    private int getDataSectionOffset() {
        return HeaderItem.ITEM_SIZE +
                stringSection.getItemCount() * StringIdItem.ITEM_SIZE +
                typeSection.getItemCount() * TypeIdItem.ITEM_SIZE +
                protoSection.getItemCount() * ProtoIdItem.ITEM_SIZE +
                fieldSection.getItemCount() * FieldIdItem.ITEM_SIZE +
                methodSection.getItemCount() * MethodIdItem.ITEM_SIZE +
                classSection.getItemCount() * ClassDefItem.ITEM_SIZE;
    }

    @NonNull
    public List<String> getMethodReferences() {
        List<String> methodReferences = Lists.newArrayList();
        for (Entry<? extends MethodRefKey, Integer> methodReference : methodSection.getItems()) {
            methodReferences.add(ReferenceUtil.getMethodDescriptor(methodReference.getKey()));
        }
        return methodReferences;
    }

    @NonNull
    public List<String> getFieldReferences() {
        List<String> fieldReferences = Lists.newArrayList();
        for (Entry<? extends FieldRefKey, Integer> fieldReference : fieldSection.getItems()) {
            fieldReferences.add(ReferenceUtil.getFieldDescriptor(fieldReference.getKey()));
        }
        return fieldReferences;
    }

    @NonNull
    public List<String> getTypeReferences() {
        List<String> classReferences = Lists.newArrayList();
        for (Entry<? extends TypeKey, Integer> typeReference : typeSection.getItems()) {
            classReferences.add(typeReference.getKey().toString());
        }
        return classReferences;
    }

    /**
     * Checks whether any of the size-sensitive constant pools have overflowed.
     * <p>
     * This checks whether the type, method, field pools are larger than 64k entries.
     * <p>
     * Note that even if this returns true, it may still be possible to successfully write the dex file, if the
     * overflowed items are not referenced anywhere that uses a 16-bit index
     *
     * @return true if any of the size-sensitive constant pools have overflowed
     */
    public boolean hasOverflowed() {
        return methodSection.getItemCount() > (1 << 16) ||
                typeSection.getItemCount() > (1 << 16) ||
                fieldSection.getItemCount() > (1 << 16);
    }

    public void writeTo(@NonNull DexDataStore dest) throws IOException {
        this.writeTo(dest, MemoryDeferredOutputStream.getFactory());
    }

    public void writeTo(@NonNull DexDataStore dest,
                        @NonNull DeferredOutputStreamFactory tempFactory) throws IOException {
        try {
            int dataSectionOffset = getDataSectionOffset();
            DexDataWriter headerWriter = outputAt(dest, 0);
            DexDataWriter indexWriter = outputAt(dest, HeaderItem.ITEM_SIZE);
            DexDataWriter offsetWriter = outputAt(dest, dataSectionOffset);
            try {
                writeStrings(indexWriter, offsetWriter);
                writeTypes(indexWriter);
                writeTypeLists(offsetWriter);
                writeProtos(indexWriter);
                writeFields(indexWriter);
                writeMethods(indexWriter);
                writeEncodedArrays(offsetWriter);
                writeAnnotations(offsetWriter);
                writeAnnotationSets(offsetWriter);
                writeAnnotationSetRefs(offsetWriter);
                writeAnnotationDirectories(offsetWriter);
                writeDebugAndCodeItems(offsetWriter, tempFactory.makeDeferredOutputStream());
                writeClasses(indexWriter, offsetWriter);
                writeMapItem(offsetWriter);
                writeHeader(headerWriter, dataSectionOffset, offsetWriter.getPosition());
            } finally {
                headerWriter.close();
                indexWriter.close();
                offsetWriter.close();
            }
            updateSignature(dest);
            updateChecksum(dest);
        } finally {
            dest.close();
        }
    }

    private void updateSignature(@NonNull DexDataStore dataStore) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        byte[] buffer = new byte[4 * 1024];
        InputStream input = dataStore.readAt(HeaderItem.SIGNATURE_DATA_START_OFFSET);
        int bytesRead = input.read(buffer);
        while (bytesRead >= 0) {
            md.update(buffer, 0, bytesRead);
            bytesRead = input.read(buffer);
        }

        byte[] signature = md.digest();
        if (signature.length != HeaderItem.SIGNATURE_SIZE) {
            throw new RuntimeException("unexpected digest write: " + signature.length + " bytes");
        }

        // write signature
        OutputStream output = dataStore.outputAt(HeaderItem.SIGNATURE_OFFSET);
        output.write(signature);
        output.close();
    }

    private void updateChecksum(@NonNull DexDataStore dataStore) throws IOException {
        Adler32 a32 = new Adler32();

        byte[] buffer = new byte[4 * 1024];
        InputStream input = dataStore.readAt(HeaderItem.CHECKSUM_DATA_START_OFFSET);
        int bytesRead = input.read(buffer);
        while (bytesRead >= 0) {
            a32.update(buffer, 0, bytesRead);
            bytesRead = input.read(buffer);
        }

        // write checksum, utilizing logic in DexWriter to write the integer value properly
        OutputStream output = dataStore.outputAt(HeaderItem.CHECKSUM_OFFSET);
        DexDataWriter.writeInt(output, (int) a32.getValue());
        output.close();
    }

    private void writeStrings(@NonNull DexDataWriter indexWriter, @NonNull DexDataWriter offsetWriter) throws IOException {
        stringIndexSectionOffset = indexWriter.getPosition();
        stringDataSectionOffset = offsetWriter.getPosition();
        int index = 0;
        List<Entry<? extends StringKey, Integer>> stringEntries = Lists.newArrayList(stringSection.getItems());
        Collections.sort(stringEntries, toStringKeyComparator);

        for (Map.Entry<? extends StringKey, Integer> entry : stringEntries) {
            entry.setValue(index++);
            indexWriter.writeInt(offsetWriter.getPosition());
            String stringValue = entry.getKey().toString();
            offsetWriter.writeUleb128(stringValue.length());
            offsetWriter.writeString(stringValue);
            offsetWriter.write(0);
        }
    }

    private void writeTypes(@NonNull DexDataWriter writer) throws IOException {
        typeSectionOffset = writer.getPosition();
        int index = 0;

        List<Map.Entry<? extends TypeKey, Integer>> typeEntries = Lists.newArrayList(typeSection.getItems());
        Collections.sort(typeEntries, toStringKeyComparator);

        for (Map.Entry<? extends TypeKey, Integer> entry : typeEntries) {
            entry.setValue(index++);
            writer.writeInt(stringSection.getItemIndex(typeSection.getString(entry.getKey())));
        }
    }

    private void writeProtos(@NonNull DexDataWriter writer) throws IOException {
        protoSectionOffset = writer.getPosition();
        int index = 0;

        List<Map.Entry<? extends ProtoRefKey, Integer>> protoEntries = Lists.newArrayList(protoSection.getItems());
        Collections.sort(protoEntries, DexWriter.<ProtoRefKey>comparableKeyComparator());

        for (Map.Entry<? extends ProtoRefKey, Integer> entry : protoEntries) {
            entry.setValue(index++);
            ProtoRefKey key = entry.getKey();
            writer.writeInt(stringSection.getItemIndex(protoSection.getShorty(key)));
            writer.writeInt(typeSection.getItemIndex(protoSection.getReturnType(key)));
            writer.writeInt(typeListSection.getNullableItemOffset(protoSection.getParameters(key)));
        }
    }

    private void writeFields(@NonNull DexDataWriter writer) throws IOException {
        fieldSectionOffset = writer.getPosition();
        int index = 0;

        List<Map.Entry<? extends FieldRefKey, Integer>> fieldEntries = Lists.newArrayList(fieldSection.getItems());
        Collections.sort(fieldEntries, DexWriter.<FieldRefKey>comparableKeyComparator());

        for (Map.Entry<? extends FieldRefKey, Integer> entry : fieldEntries) {
            entry.setValue(index++);
            FieldRefKey key = entry.getKey();
            writer.writeUshort(typeSection.getItemIndex(fieldSection.getDefiningClass(key)));
            writer.writeUshort(typeSection.getItemIndex(fieldSection.getFieldType(key)));
            writer.writeInt(stringSection.getItemIndex(fieldSection.getName(key)));
        }
    }

    private void writeMethods(@NonNull DexDataWriter writer) throws IOException {
        methodSectionOffset = writer.getPosition();
        int index = 0;

        List<Map.Entry<? extends MethodRefKey, Integer>> methodEntries = Lists.newArrayList(methodSection.getItems());
        Collections.sort(methodEntries, DexWriter.<MethodRefKey>comparableKeyComparator());

        for (Map.Entry<? extends MethodRefKey, Integer> entry : methodEntries) {
            entry.setValue(index++);
            MethodRefKey key = entry.getKey();
            writer.writeUshort(typeSection.getItemIndex(methodSection.getDefiningClass(key)));
            writer.writeUshort(protoSection.getItemIndex(methodSection.getPrototype(key)));
            writer.writeInt(stringSection.getItemIndex(methodSection.getName(key)));
        }
    }

    private void writeClasses(@NonNull DexDataWriter indexWriter, @NonNull DexDataWriter offsetWriter) throws IOException {
        classIndexSectionOffset = indexWriter.getPosition();
        classDataSectionOffset = offsetWriter.getPosition();

        List<Map.Entry<? extends ClassKey, Integer>> classEntries = Lists.newArrayList(classSection.getItems());
        Collections.sort(classEntries, DexWriter.<ClassKey>comparableKeyComparator());

        int index = 0;
        for (Map.Entry<? extends ClassKey, Integer> key : classEntries) {
            index = writeClass(indexWriter, offsetWriter, index, key);
        }
    }

    /**
     * Writes out the class_def_item and class_data_item for the given class.
     * <p>
     * This will recursively write out any unwritten superclass/interface before writing the class itself, as per the
     * dex specification.
     *
     * @return the index for the next class to be written
     */
    private int writeClass(@NonNull DexDataWriter indexWriter, @NonNull DexDataWriter offsetWriter,
                           int nextIndex, @Nullable Map.Entry<? extends ClassKey, Integer> entry) throws IOException {
        if (entry == null) {
            // class does not exist in this dex file, cannot write it
            return nextIndex;
        }

        if (entry.getValue() != NO_INDEX) {
            // class has already been written, no need to write it
            return nextIndex;
        }

        ClassKey key = entry.getKey();

        // set a bogus index, to make sure we don't recurse and double-write it
        entry.setValue(0);

        // first, try to write the superclass
        Map.Entry<? extends ClassKey, Integer> superEntry =
                classSection.getClassEntryByType(classSection.getSuperclass(key));
        nextIndex = writeClass(indexWriter, offsetWriter, nextIndex, superEntry);

        // then, try to write interfaces
        for (TypeKey interfaceTypeKey : typeListSection.getTypes(classSection.getInterfaces(key))) {
            Map.Entry<? extends ClassKey, Integer> interfaceEntry = classSection.getClassEntryByType(interfaceTypeKey);
            nextIndex = writeClass(indexWriter, offsetWriter, nextIndex, interfaceEntry);
        }

        // now set the index for real
        entry.setValue(nextIndex++);

        // and finally, write the class itself
        // first, the class_def_item
        indexWriter.writeInt(typeSection.getItemIndex(classSection.getType(key)));
        indexWriter.writeInt(classSection.getAccessFlags(key));
        indexWriter.writeInt(typeSection.getNullableItemIndex(classSection.getSuperclass(key)));
        indexWriter.writeInt(typeListSection.getNullableItemOffset(classSection.getInterfaces(key)));
        indexWriter.writeInt(stringSection.getNullableItemIndex(classSection.getSourceFile(key)));
        indexWriter.writeInt(classSection.getAnnotationDirectoryOffset(key));

        Collection<? extends FieldKey> staticFields = classSection.getSortedStaticFields(key);
        Collection<? extends FieldKey> instanceFields = classSection.getSortedInstanceFields(key);
        Collection<? extends MethodKey> directMethods = classSection.getSortedDirectMethods(key);
        Collection<? extends MethodKey> virtualMethods = classSection.getSortedVirtualMethods(key);
        boolean classHasData = staticFields.size() > 0 ||
                instanceFields.size() > 0 ||
                directMethods.size() > 0 ||
                virtualMethods.size() > 0;

        if (classHasData) {
            indexWriter.writeInt(offsetWriter.getPosition());
        } else {
            indexWriter.writeInt(0);
        }

        indexWriter.writeInt(classSection.getEncodedArrayOffset(key));

        // now write the class_data_item
        if (classHasData) {
            numClassDataItems++;

            offsetWriter.writeUleb128(staticFields.size());
            offsetWriter.writeUleb128(instanceFields.size());
            offsetWriter.writeUleb128(directMethods.size());
            offsetWriter.writeUleb128(virtualMethods.size());

            writeEncodedFields(offsetWriter, staticFields);
            writeEncodedFields(offsetWriter, instanceFields);
            writeEncodedMethods(offsetWriter, directMethods);
            writeEncodedMethods(offsetWriter, virtualMethods);
        }

        return nextIndex;
    }

    private void writeEncodedFields(@NonNull DexDataWriter writer, @NonNull Collection<? extends FieldKey> fields)
            throws IOException {
        int prevIndex = 0;
        for (FieldKey key : fields) {
            int index = fieldSection.getFieldIndex(key);
            writer.writeUleb128(index - prevIndex);
            writer.writeUleb128(classSection.getFieldAccessFlags(key));
            prevIndex = index;
        }
    }

    private void writeEncodedMethods(@NonNull DexDataWriter writer, @NonNull Collection<? extends MethodKey> methods)
            throws IOException {
        int prevIndex = 0;
        for (MethodKey key : methods) {
            int index = methodSection.getMethodIndex(key);
            writer.writeUleb128(index - prevIndex);
            writer.writeUleb128(classSection.getMethodAccessFlags(key));
            writer.writeUleb128(classSection.getCodeItemOffset(key));
            prevIndex = index;
        }
    }

    private void writeTypeLists(@NonNull DexDataWriter writer) throws IOException {
        writer.align();
        typeListSectionOffset = writer.getPosition();
        for (Map.Entry<? extends TypeListKey, Integer> entry : typeListSection.getItems()) {
            writer.align();
            entry.setValue(writer.getPosition());

            Collection<? extends TypeKey> types = typeListSection.getTypes(entry.getKey());
            writer.writeInt(types.size());
            for (TypeKey typeKey : types) {
                writer.writeUshort(typeSection.getItemIndex(typeKey));
            }
        }
    }

    private void writeEncodedArrays(@NonNull DexDataWriter writer) throws IOException {
        InternalEncodedValueWriter encodedValueWriter = new InternalEncodedValueWriter(writer);
        encodedArraySectionOffset = writer.getPosition();

        HashMap<EncodedArrayKey<EncodedValue>, Integer> internedItems = Maps.newHashMap();
        EncodedArrayKey<EncodedValue> key = new EncodedArrayKey<EncodedValue>();

        for (ClassKey classKey : classSection.getSortedClasses()) {
            Collection<? extends EncodedValue> elements = classSection.getStaticInitializers(classKey);
            if (elements != null && elements.size() > 0) {
                key.elements = elements;
                Integer prev = internedItems.get(key);
                if (prev != null) {
                    classSection.setEncodedArrayOffset(classKey, prev);
                } else {
                    int offset = writer.getPosition();
                    internedItems.put(key, offset);
                    classSection.setEncodedArrayOffset(classKey, offset);
                    key = new EncodedArrayKey<EncodedValue>();

                    numEncodedArrayItems++;

                    writer.writeUleb128(elements.size());
                    for (EncodedValue value : elements) {
                        writeEncodedValue(encodedValueWriter, value);
                    }
                }
            }
        }
    }

    private void writeAnnotations(@NonNull DexDataWriter writer) throws IOException {
        InternalEncodedValueWriter encodedValueWriter = new InternalEncodedValueWriter(writer);

        annotationSectionOffset = writer.getPosition();
        for (Map.Entry<? extends AnnotationKey, Integer> entry : annotationSection.getItems()) {
            entry.setValue(writer.getPosition());

            AnnotationKey key = entry.getKey();

            writer.writeUbyte(annotationSection.getVisibility(key));
            writer.writeUleb128(typeSection.getItemIndex(annotationSection.getType(key)));

            Collection<? extends AnnotationElement> elements = Ordering.from(BaseAnnotationElement.BY_NAME)
                    .immutableSortedCopy(annotationSection.getElements(key));

            writer.writeUleb128(elements.size());

            for (AnnotationElement element : elements) {
                writer.writeUleb128(stringSection.getItemIndex(annotationSection.getElementName(element)));
                writeEncodedValue(encodedValueWriter, annotationSection.getElementValue(element));
            }
        }
    }

    private void writeAnnotationSets(@NonNull DexDataWriter writer) throws IOException {
        writer.align();
        annotationSetSectionOffset = writer.getPosition();
        if (shouldCreateEmptyAnnotationSet()) {
            writer.writeInt(0);
        }
        for (Map.Entry<? extends AnnotationSetKey, Integer> entry : annotationSetSection.getItems()) {
            Collection<? extends AnnotationKey> annotations = Ordering.from(BaseAnnotation.BY_TYPE)
                    .immutableSortedCopy(annotationSetSection.getAnnotations(entry.getKey()));

            writer.align();
            entry.setValue(writer.getPosition());
            writer.writeInt(annotations.size());
            for (AnnotationKey annotationKey : annotations) {
                writer.writeInt(annotationSection.getItemOffset(annotationKey));
            }
        }
    }

    private void writeAnnotationSetRefs(@NonNull DexDataWriter writer) throws IOException {
        writer.align();
        annotationSetRefSectionOffset = writer.getPosition();
        HashMap<List<? extends AnnotationSetKey>, Integer> internedItems = Maps.newHashMap();

        for (ClassKey classKey : classSection.getSortedClasses()) {
            for (MethodKey methodKey : classSection.getSortedMethods(classKey)) {
                List<? extends AnnotationSetKey> parameterAnnotations = classSection.getParameterAnnotations(methodKey);
                if (parameterAnnotations != null) {
                    Integer prev = internedItems.get(parameterAnnotations);
                    if (prev != null) {
                        classSection.setAnnotationSetRefListOffset(methodKey, prev);
                    } else {
                        writer.align();
                        int position = writer.getPosition();
                        classSection.setAnnotationSetRefListOffset(methodKey, position);
                        internedItems.put(parameterAnnotations, position);

                        numAnnotationSetRefItems++;

                        writer.writeInt(parameterAnnotations.size());
                        for (AnnotationSetKey annotationSetKey : parameterAnnotations) {
                            if (annotationSetSection.getAnnotations(annotationSetKey).size() > 0) {
                                writer.writeInt(annotationSetSection.getItemOffset(annotationSetKey));
                            } else if (shouldCreateEmptyAnnotationSet()) {
                                writer.writeInt(annotationSetSectionOffset);
                            } else {
                                writer.writeInt(NO_OFFSET);
                            }
                        }
                    }
                }
            }
        }
    }

    private void writeAnnotationDirectories(@NonNull DexDataWriter writer) throws IOException {
        writer.align();
        annotationDirectorySectionOffset = writer.getPosition();
        HashMap<AnnotationSetKey, Integer> internedItems = Maps.newHashMap();

        ByteBuffer tempBuffer = ByteBuffer.allocate(65536);
        tempBuffer.order(ByteOrder.LITTLE_ENDIAN);

        for (ClassKey key : classSection.getSortedClasses()) {
            // first, we write the field/method/parameter items to a temporary buffer, so that we can get a count
            // of each type, and determine if we even need to write an annotation directory for this class

            Collection<? extends FieldKey> fields = classSection.getSortedFields(key);
            Collection<? extends MethodKey> methods = classSection.getSortedMethods(key);

            // this is how much space we'll need if every field and method has annotations.
            int maxSize = fields.size() * 8 + methods.size() * 16;
            if (maxSize > tempBuffer.capacity()) {
                tempBuffer = ByteBuffer.allocate(maxSize);
                tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
            }

            tempBuffer.clear();

            int fieldAnnotations = 0;
            int methodAnnotations = 0;
            int parameterAnnotations = 0;

            for (FieldKey field : fields) {
                AnnotationSetKey fieldAnnotationsKey = classSection.getFieldAnnotations(field);
                if (fieldAnnotationsKey != null) {
                    fieldAnnotations++;
                    tempBuffer.putInt(fieldSection.getFieldIndex(field));
                    tempBuffer.putInt(annotationSetSection.getItemOffset(fieldAnnotationsKey));
                }
            }

            for (MethodKey method : methods) {
                AnnotationSetKey methodAnnotationsKey = classSection.getMethodAnnotations(method);
                if (methodAnnotationsKey != null) {
                    methodAnnotations++;
                    tempBuffer.putInt(methodSection.getMethodIndex(method));
                    tempBuffer.putInt(annotationSetSection.getItemOffset(methodAnnotationsKey));
                }
            }

            for (MethodKey method : methods) {
                int offset = classSection.getAnnotationSetRefListOffset(method);
                if (offset != DexWriter.NO_OFFSET) {
                    parameterAnnotations++;
                    tempBuffer.putInt(methodSection.getMethodIndex(method));
                    tempBuffer.putInt(offset);
                }
            }

            // now, we finally know how many field/method/parameter annotations were written to the temp buffer

            AnnotationSetKey classAnnotationKey = classSection.getClassAnnotations(key);
            if (fieldAnnotations == 0 && methodAnnotations == 0 && parameterAnnotations == 0) {
                if (classAnnotationKey != null) {
                    // This is an internable directory. Let's see if we've already written one like it
                    Integer directoryOffset = internedItems.get(classAnnotationKey);
                    if (directoryOffset != null) {
                        classSection.setAnnotationDirectoryOffset(key, directoryOffset);
                        continue;
                    } else {
                        internedItems.put(classAnnotationKey, writer.getPosition());
                    }
                } else {
                    continue;
                }
            }

            // yep, we need to write it out
            numAnnotationDirectoryItems++;
            classSection.setAnnotationDirectoryOffset(key, writer.getPosition());

            writer.writeInt(annotationSetSection.getNullableItemOffset(classAnnotationKey));
            writer.writeInt(fieldAnnotations);
            writer.writeInt(methodAnnotations);
            writer.writeInt(parameterAnnotations);
            writer.write(tempBuffer.array(), 0, tempBuffer.position());
        }
    }

    private void writeDebugAndCodeItems(@NonNull DexDataWriter offsetWriter,
                                        @NonNull DeferredOutputStream temp) throws IOException {
        ByteArrayOutputStream ehBuf = new ByteArrayOutputStream();
        debugSectionOffset = offsetWriter.getPosition();
        DebugWriter<StringKey, TypeKey> debugWriter =
                new DebugWriter<StringKey, TypeKey>(stringSection, typeSection, offsetWriter);

        DexDataWriter codeWriter = new DexDataWriter(temp, 0);

        List<CodeItemOffset<MethodKey>> codeOffsets = Lists.newArrayList();

        for (ClassKey classKey : classSection.getSortedClasses()) {
            Collection<? extends MethodKey> directMethods = classSection.getSortedDirectMethods(classKey);
            Collection<? extends MethodKey> virtualMethods = classSection.getSortedVirtualMethods(classKey);

            Iterable<MethodKey> methods = Iterables.concat(directMethods, virtualMethods);

            for (MethodKey methodKey : methods) {
                List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks =
                        classSection.getTryBlocks(methodKey);
                Iterable<? extends Instruction> instructions = classSection.getInstructions(methodKey);
                Iterable<? extends DebugItem> debugItems = classSection.getDebugItems(methodKey);

                if (instructions != null && stringSection.hasJumboIndexes()) {
                    boolean needsFix = false;
                    for (Instruction instruction : instructions) {
                        if (instruction.getOpcode() == Opcode.CONST_STRING) {
                            if (stringSection.getItemIndex(
                                    (StringRef) ((ReferenceInstruction) instruction).getReference()) >= 65536) {
                                needsFix = true;
                                break;
                            }
                        }
                    }

                    if (needsFix) {
                        MutableMethodImplementation mutableMethodImplementation =
                                classSection.makeMutableMethodImplementation(methodKey);
                        fixInstructions(mutableMethodImplementation);

                        instructions = mutableMethodImplementation.getInstructions();
                        tryBlocks = mutableMethodImplementation.getTryBlocks();
                        debugItems = mutableMethodImplementation.getDebugItems();
                    }
                }

                int debugItemOffset = writeDebugItem(offsetWriter, debugWriter,
                        classSection.getParameterNames(methodKey), debugItems);
                int codeItemOffset;
                try {
                    codeItemOffset = writeCodeItem(
                            codeWriter, ehBuf, methodKey, tryBlocks, instructions, debugItemOffset);
                } catch (RuntimeException ex) {
                    throw new ExceptionWithContext(ex, "Exception occurred while writing code_item for method %s",
                            methodSection.getMethodReference(methodKey));
                }

                if (codeItemOffset != -1) {
                    codeOffsets.add(new CodeItemOffset<MethodKey>(methodKey, codeItemOffset));
                }
            }
        }

        offsetWriter.align();
        codeSectionOffset = offsetWriter.getPosition();

        codeWriter.close();
        temp.writeTo(offsetWriter);
        temp.close();

        for (CodeItemOffset<MethodKey> codeOffset : codeOffsets) {
            classSection.setCodeItemOffset(codeOffset.method, codeSectionOffset + codeOffset.codeOffset);
        }
    }

    private void fixInstructions(@NonNull MutableMethodImplementation methodImplementation) {
        List<? extends Instruction> instructions = methodImplementation.getInstructions();

        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);

            if (instruction.getOpcode() == Opcode.CONST_STRING) {
                if (stringSection.getItemIndex(
                        (StringRef) ((ReferenceInstruction) instruction).getReference()) >= 65536) {
                    methodImplementation.replaceInstruction(i, new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                            ((OneRegisterInstruction) instruction).getRegisterA(),
                            ((ReferenceInstruction) instruction).getReference()));
                }
            }
        }
    }

    private int writeDebugItem(@NonNull DexDataWriter writer,
                               @NonNull DebugWriter<StringKey, TypeKey> debugWriter,
                               @Nullable Iterable<? extends StringKey> parameterNames,
                               @Nullable Iterable<? extends DebugItem> debugItems) throws IOException {
        int parameterCount = 0;
        int lastNamedParameterIndex = -1;
        if (parameterNames != null) {
            parameterCount = Iterables.size(parameterNames);
            int index = 0;
            for (StringKey parameterName : parameterNames) {
                if (parameterName != null) {
                    lastNamedParameterIndex = index;
                }
                index++;
            }
        }


        if (lastNamedParameterIndex == -1 && (debugItems == null || Iterables.isEmpty(debugItems))) {
            return NO_OFFSET;
        }

        numDebugInfoItems++;

        int debugItemOffset = writer.getPosition();
        int startingLineNumber = 0;

        if (debugItems != null) {
            for (org.jf.dexlib2.iface.debug.DebugItem debugItem : debugItems) {
                if (debugItem instanceof LineNumber) {
                    startingLineNumber = ((LineNumber) debugItem).getLineNumber();
                    break;
                }
            }
        }
        writer.writeUleb128(startingLineNumber);

        writer.writeUleb128(parameterCount);
        if (parameterNames != null) {
            int index = 0;
            for (StringKey parameterName : parameterNames) {
                if (index == parameterCount) {
                    break;
                }
                index++;
                writer.writeUleb128(stringSection.getNullableItemIndex(parameterName) + 1);
            }
        }

        if (debugItems != null) {
            debugWriter.reset(startingLineNumber);

            for (DebugItem debugItem : debugItems) {
                classSection.writeDebugItem(debugWriter, debugItem);
            }
        }
        // write an END_SEQUENCE opcode, to end the debug item
        writer.write(0);

        return debugItemOffset;
    }

    private int writeCodeItem(@NonNull DexDataWriter writer,
                              @NonNull ByteArrayOutputStream ehBuf,
                              @NonNull MethodKey methodKey,
                              @NonNull List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks,
                              @Nullable Iterable<? extends Instruction> instructions,
                              int debugItemOffset) throws IOException {
        if (instructions == null && debugItemOffset == NO_OFFSET) {
            return -1;
        }

        numCodeItemItems++;

        writer.align();

        int codeItemOffset = writer.getPosition();

        writer.writeUshort(classSection.getRegisterCount(methodKey));

        boolean isStatic = AccessFlags.STATIC.isSet(classSection.getMethodAccessFlags(methodKey));
        Collection<? extends TypeKey> parameters = typeListSection.getTypes(
                protoSection.getParameters(methodSection.getPrototype(methodKey)));

        writer.writeUshort(MethodUtil.getParameterRegisterCount(parameters, isStatic));

        if (instructions != null) {
            tryBlocks = TryListBuilder.massageTryBlocks(tryBlocks);

            int outParamCount = 0;
            int codeUnitCount = 0;
            for (Instruction instruction : instructions) {
                codeUnitCount += instruction.getCodeUnits();
                if (instruction.getOpcode().referenceType == ReferenceType.METHOD) {
                    ReferenceInstruction refInsn = (ReferenceInstruction) instruction;
                    MethodReference methodRef = (MethodReference) refInsn.getReference();
                    Opcode opcode = instruction.getOpcode();
                    int paramCount;
                    if (InstructionUtil.isInvokePolymorphic(opcode)) {
                        paramCount = ((VariableRegisterInstruction) instruction).getRegisterCount();
                    } else {
                        paramCount = MethodUtil.getParameterRegisterCount(methodRef, InstructionUtil.isInvokeStatic(opcode));
                    }
                    if (paramCount > outParamCount) {
                        outParamCount = paramCount;
                    }
                }
            }

            writer.writeUshort(outParamCount);
            writer.writeUshort(tryBlocks.size());
            writer.writeInt(debugItemOffset);

            InstructionWriter instructionWriter =
                    InstructionWriter.makeInstructionWriter(opcodes, writer, stringSection, typeSection, fieldSection,
                            methodSection, protoSection);

            writer.writeInt(codeUnitCount);
            int codeOffset = 0;
            for (Instruction instruction : instructions) {
                try {
                    switch (instruction.getOpcode().format) {
                        case Format10t:
                            instructionWriter.write((Instruction10t) instruction);
                            break;
                        case Format10x:
                            instructionWriter.write((Instruction10x) instruction);
                            break;
                        case Format11n:
                            instructionWriter.write((Instruction11n) instruction);
                            break;
                        case Format11x:
                            instructionWriter.write((Instruction11x) instruction);
                            break;
                        case Format12x:
                            instructionWriter.write((Instruction12x) instruction);
                            break;
                        case Format20bc:
                            instructionWriter.write((Instruction20bc) instruction);
                            break;
                        case Format20t:
                            instructionWriter.write((Instruction20t) instruction);
                            break;
                        case Format21c:
                            instructionWriter.write((Instruction21c) instruction);
                            break;
                        case Format21ih:
                            instructionWriter.write((Instruction21ih) instruction);
                            break;
                        case Format21lh:
                            instructionWriter.write((Instruction21lh) instruction);
                            break;
                        case Format21s:
                            instructionWriter.write((Instruction21s) instruction);
                            break;
                        case Format21t:
                            instructionWriter.write((Instruction21t) instruction);
                            break;
                        case Format22b:
                            instructionWriter.write((Instruction22b) instruction);
                            break;
                        case Format22c:
                            instructionWriter.write((Instruction22c) instruction);
                            break;
                        case Format22cs:
                            instructionWriter.write((Instruction22cs) instruction);
                            break;
                        case Format22s:
                            instructionWriter.write((Instruction22s) instruction);
                            break;
                        case Format22t:
                            instructionWriter.write((Instruction22t) instruction);
                            break;
                        case Format22x:
                            instructionWriter.write((Instruction22x) instruction);
                            break;
                        case Format23x:
                            instructionWriter.write((Instruction23x) instruction);
                            break;
                        case Format30t:
                            instructionWriter.write((Instruction30t) instruction);
                            break;
                        case Format31c:
                            instructionWriter.write((Instruction31c) instruction);
                            break;
                        case Format31i:
                            instructionWriter.write((Instruction31i) instruction);
                            break;
                        case Format31t:
                            instructionWriter.write((Instruction31t) instruction);
                            break;
                        case Format32x:
                            instructionWriter.write((Instruction32x) instruction);
                            break;
                        case Format35c:
                            instructionWriter.write((Instruction35c) instruction);
                            break;
                        case Format35mi:
                            instructionWriter.write((Instruction35mi) instruction);
                            break;
                        case Format35ms:
                            instructionWriter.write((Instruction35ms) instruction);
                            break;
                        case Format3rc:
                            instructionWriter.write((Instruction3rc) instruction);
                            break;
                        case Format3rmi:
                            instructionWriter.write((Instruction3rmi) instruction);
                            break;
                        case Format3rms:
                            instructionWriter.write((Instruction3rms) instruction);
                            break;
                        case Format45cc:
                            instructionWriter.write((Instruction45cc) instruction);
                            break;
                        case Format4rcc:
                            instructionWriter.write((Instruction4rcc) instruction);
                            break;
                        case Format51l:
                            instructionWriter.write((Instruction51l) instruction);
                            break;
                        case ArrayPayload:
                            instructionWriter.write((ArrayPayload) instruction);
                            break;
                        case PackedSwitchPayload:
                            instructionWriter.write((PackedSwitchPayload) instruction);
                            break;
                        case SparseSwitchPayload:
                            instructionWriter.write((SparseSwitchPayload) instruction);
                            break;
                        default:
                            throw new ExceptionWithContext("Unsupported instruction format: %s",
                                    instruction.getOpcode().format);
                    }
                } catch (RuntimeException ex) {
                    throw new ExceptionWithContext(ex, "Error while writing instruction at code offset 0x%x", codeOffset);
                }
                codeOffset += instruction.getCodeUnits();
            }

            if (tryBlocks.size() > 0) {
                writer.align();

                // filter out unique lists of exception handlers
                Map<List<? extends ExceptionHandler>, Integer> exceptionHandlerOffsetMap = Maps.newHashMap();
                for (TryBlock<? extends ExceptionHandler> tryBlock : tryBlocks) {
                    exceptionHandlerOffsetMap.put(tryBlock.getExceptionHandlers(), 0);
                }
                DexDataWriter.writeUleb128(ehBuf, exceptionHandlerOffsetMap.size());

                for (TryBlock<? extends ExceptionHandler> tryBlock : tryBlocks) {
                    int startAddress = tryBlock.getStartCodeAddress();
                    int endAddress = startAddress + tryBlock.getCodeUnitCount();

                    int tbCodeUnitCount = endAddress - startAddress;

                    writer.writeInt(startAddress);
                    writer.writeUshort(tbCodeUnitCount);

                    if (tryBlock.getExceptionHandlers().size() == 0) {
                        throw new ExceptionWithContext("No exception handlers for the try block!");
                    }

                    Integer offset = exceptionHandlerOffsetMap.get(tryBlock.getExceptionHandlers());
                    if (offset != 0) {
                        // exception handler has already been written out, just use it
                        writer.writeUshort(offset);
                    } else {
                        // if offset has not been set yet, we are about to write out a new exception handler
                        offset = ehBuf.size();
                        writer.writeUshort(offset);
                        exceptionHandlerOffsetMap.put(tryBlock.getExceptionHandlers(), offset);

                        // check if the last exception handler is a catch-all and adjust the size accordingly
                        int ehSize = tryBlock.getExceptionHandlers().size();
                        ExceptionHandler ehLast = tryBlock.getExceptionHandlers().get(ehSize - 1);
                        if (ehLast.getExceptionType() == null) {
                            ehSize = ehSize * (-1) + 1;
                        }

                        // now let's layout the exception handlers, assuming that catch-all is always last
                        DexDataWriter.writeSleb128(ehBuf, ehSize);
                        for (ExceptionHandler eh : tryBlock.getExceptionHandlers()) {
                            TypeKey exceptionTypeKey = classSection.getExceptionType(eh);

                            int codeAddress = eh.getHandlerCodeAddress();

                            if (exceptionTypeKey != null) {
                                //regular exception handling
                                DexDataWriter.writeUleb128(ehBuf, typeSection.getItemIndex(exceptionTypeKey));
                                DexDataWriter.writeUleb128(ehBuf, codeAddress);
                            } else {
                                //catch-all
                                DexDataWriter.writeUleb128(ehBuf, codeAddress);
                            }
                        }
                    }
                }

                if (ehBuf.size() > 0) {
                    ehBuf.writeTo(writer);
                    ehBuf.reset();
                }
            }
        } else {
            // no instructions, all we have is the debug item offset
            writer.writeUshort(0);
            writer.writeUshort(0);
            writer.writeInt(debugItemOffset);
            writer.writeInt(0);
        }

        return codeItemOffset;
    }

    private int calcNumItems() {
        int numItems = 0;

        // header item
        numItems++;

        if (stringSection.getItems().size() > 0) {
            numItems += 2; // index and data
        }
        if (typeSection.getItems().size() > 0) {
            numItems++;
        }
        if (protoSection.getItems().size() > 0) {
            numItems++;
        }
        if (fieldSection.getItems().size() > 0) {
            numItems++;
        }
        if (methodSection.getItems().size() > 0) {
            numItems++;
        }
        if (typeListSection.getItems().size() > 0) {
            numItems++;
        }
        if (numEncodedArrayItems > 0) {
            numItems++;
        }
        if (annotationSection.getItems().size() > 0) {
            numItems++;
        }
        if (annotationSetSection.getItems().size() > 0 || shouldCreateEmptyAnnotationSet()) {
            numItems++;
        }
        if (numAnnotationSetRefItems > 0) {
            numItems++;
        }
        if (numAnnotationDirectoryItems > 0) {
            numItems++;
        }
        if (numDebugInfoItems > 0) {
            numItems++;
        }
        if (numCodeItemItems > 0) {
            numItems++;
        }
        if (classSection.getItems().size() > 0) {
            numItems++;
        }
        if (numClassDataItems > 0) {
            numItems++;
        }
        // map item itself
        numItems++;

        return numItems;
    }

    private void writeMapItem(@NonNull DexDataWriter writer) throws IOException {
        writer.align();
        mapSectionOffset = writer.getPosition();
        int numItems = calcNumItems();

        writer.writeInt(numItems);

        // index section
        writeMapItem(writer, ItemType.HEADER_ITEM, 1, 0);
        writeMapItem(writer, ItemType.STRING_ID_ITEM, stringSection.getItems().size(), stringIndexSectionOffset);
        writeMapItem(writer, ItemType.TYPE_ID_ITEM, typeSection.getItems().size(), typeSectionOffset);
        writeMapItem(writer, ItemType.PROTO_ID_ITEM, protoSection.getItems().size(), protoSectionOffset);
        writeMapItem(writer, ItemType.FIELD_ID_ITEM, fieldSection.getItems().size(), fieldSectionOffset);
        writeMapItem(writer, ItemType.METHOD_ID_ITEM, methodSection.getItems().size(), methodSectionOffset);
        writeMapItem(writer, ItemType.CLASS_DEF_ITEM, classSection.getItems().size(), classIndexSectionOffset);

        // data section
        writeMapItem(writer, ItemType.STRING_DATA_ITEM, stringSection.getItems().size(), stringDataSectionOffset);
        writeMapItem(writer, ItemType.TYPE_LIST, typeListSection.getItems().size(), typeListSectionOffset);
        writeMapItem(writer, ItemType.ENCODED_ARRAY_ITEM, numEncodedArrayItems, encodedArraySectionOffset);
        writeMapItem(writer, ItemType.ANNOTATION_ITEM, annotationSection.getItems().size(), annotationSectionOffset);
        writeMapItem(writer, ItemType.ANNOTATION_SET_ITEM,
                annotationSetSection.getItems().size() + (shouldCreateEmptyAnnotationSet() ? 1 : 0), annotationSetSectionOffset);
        writeMapItem(writer, ItemType.ANNOTATION_SET_REF_LIST, numAnnotationSetRefItems, annotationSetRefSectionOffset);
        writeMapItem(writer, ItemType.ANNOTATION_DIRECTORY_ITEM, numAnnotationDirectoryItems,
                annotationDirectorySectionOffset);
        writeMapItem(writer, ItemType.DEBUG_INFO_ITEM, numDebugInfoItems, debugSectionOffset);
        writeMapItem(writer, ItemType.CODE_ITEM, numCodeItemItems, codeSectionOffset);
        writeMapItem(writer, ItemType.CLASS_DATA_ITEM, numClassDataItems, classDataSectionOffset);
        writeMapItem(writer, ItemType.MAP_LIST, 1, mapSectionOffset);
    }

    private void writeMapItem(@NonNull DexDataWriter writer, int type, int size, int offset) throws IOException {
        if (size > 0) {
            writer.writeUshort(type);
            writer.writeUshort(0);
            writer.writeInt(size);
            writer.writeInt(offset);
        }
    }

    private void writeHeader(@NonNull DexDataWriter writer, int dataOffset, int fileSize) throws IOException {
        // Write the appropriate header.
        writer.write(HeaderItem.getMagicForApi(opcodes.api));

        // checksum placeholder
        writer.writeInt(0);

        // signature placeholder
        writer.write(new byte[20]);

        writer.writeInt(fileSize);
        writer.writeInt(HeaderItem.ITEM_SIZE);
        writer.writeInt(HeaderItem.LITTLE_ENDIAN_TAG);

        // link
        writer.writeInt(0);
        writer.writeInt(0);

        // map
        writer.writeInt(mapSectionOffset);

        // index sections

        writeSectionInfo(writer, stringSection.getItems().size(), stringIndexSectionOffset);
        writeSectionInfo(writer, typeSection.getItems().size(), typeSectionOffset);
        writeSectionInfo(writer, protoSection.getItems().size(), protoSectionOffset);
        writeSectionInfo(writer, fieldSection.getItems().size(), fieldSectionOffset);
        writeSectionInfo(writer, methodSection.getItems().size(), methodSectionOffset);
        writeSectionInfo(writer, classSection.getItems().size(), classIndexSectionOffset);

        // data section
        writer.writeInt(fileSize - dataOffset);
        writer.writeInt(dataOffset);
    }

    private void writeSectionInfo(DexDataWriter writer, int numItems, int offset) throws IOException {
        writer.writeInt(numItems);
        if (numItems > 0) {
            writer.writeInt(offset);
        } else {
            writer.writeInt(0);
        }
    }

    private boolean shouldCreateEmptyAnnotationSet() {
        // Workaround for a crash in Dalvik VM before Jelly Bean MR1 (4.2)
        // which is triggered by NO_OFFSET in parameter annotation list.
        // (https://code.google.com/p/android/issues/detail?id=35304)
        return (opcodes.api < 17);
    }

    private static class EncodedArrayKey<EncodedValue> {
        @NonNull
        Collection<? extends EncodedValue> elements;

        public EncodedArrayKey() {
        }

        @Override
        public int hashCode() {
            return CollectionUtils.listHashCode(elements);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof EncodedArrayKey) {
                EncodedArrayKey other = (EncodedArrayKey) o;
                if (elements.size() != other.elements.size()) {
                    return false;
                }
                return Iterables.elementsEqual(elements, other.elements);
            }
            return false;
        }
    }

    private static class CodeItemOffset<MethodKey> {
        @NonNull
        MethodKey method;
        int codeOffset;

        private CodeItemOffset(@NonNull MethodKey method, int codeOffset) {
            this.codeOffset = codeOffset;
            this.method = method;
        }
    }

    protected class InternalEncodedValueWriter extends EncodedValueWriter<StringKey, TypeKey, FieldRefKey, MethodRefKey,
            AnnotationElement, EncodedValue> {
        private InternalEncodedValueWriter(@NonNull DexDataWriter writer) {
            super(writer, stringSection, typeSection, fieldSection, methodSection, annotationSection);
        }

        @Override
        protected void writeEncodedValue(@NonNull EncodedValue encodedValue) throws IOException {
            DexWriter.this.writeEncodedValue(this, encodedValue);
        }
    }

    public abstract class SectionProvider {
        @NonNull
        public abstract StringSectionType getStringSection();

        @NonNull
        public abstract TypeSectionType getTypeSection();

        @NonNull
        public abstract ProtoSectionType getProtoSection();

        @NonNull
        public abstract FieldSectionType getFieldSection();

        @NonNull
        public abstract MethodSectionType getMethodSection();

        @NonNull
        public abstract ClassSectionType getClassSection();

        @NonNull
        public abstract TypeListSectionType getTypeListSection();

        @NonNull
        public abstract AnnotationSectionType getAnnotationSection();

        @NonNull
        public abstract AnnotationSetSectionType getAnnotationSetSection();
    }
}
