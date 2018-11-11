package org.jnbt;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class XMLTransformerTest {
    @Test
    @SuppressWarnings("unchecked") // Guaranteed by implementation
    public void testXMLTransformer() throws IOException {
        ByteTag byteTag = new ByteTag("tag1", (byte) 1);
        ByteArrayTag byteArrayTag = new ByteArrayTag("tag2", "short".getBytes(Charsets.UTF_8));
        ByteArrayTag byteArrayTag2 = new ByteArrayTag("tag3", "Very long text which is certain to be shorter when compressed. Another sentence is apparently needed to bring it over the edge.".getBytes(Charsets.UTF_8));
        ShortTag shortTag = new ShortTag("tag4", (short) 2);
        IntTag intTag = new IntTag("tag5", 3);
        IntArrayTag intArrayTag = new IntArrayTag("tag6", new int[] {1, 1, 2, 3, 5, 8, 13});
        FloatTag floatTag = new FloatTag("tag7", 4.5f);
        LongTag longTag = new LongTag("tag8", 6L);
        LongArrayTag longArrayTag = new LongArrayTag("tag9", new long[] {8L, 13L, 21L, 34L, 55L});
        DoubleTag doubleTag = new DoubleTag("tag10", 7.8);
        StringTag stringTag = new StringTag("tag11", "Are < & > properly escaped? How about\nnewlines?");
        ListTag<StringTag> listTag = new ListTag<>("tag12", StringTag.class, Arrays.asList(new StringTag("tag13", "One"), new StringTag("tag14", "Two"), new StringTag("tag15", "Three")));
        CompoundTag compoundTag = new CompoundTag("tag0", ImmutableMap.<String, Tag>builder()
                .put(byteTag.getName(), byteTag)
                .put(byteArrayTag.getName(), byteArrayTag)
                .put(byteArrayTag2.getName(), byteArrayTag2)
                .put(shortTag.getName(), shortTag)
                .put(intTag.getName(), intTag)
                .put(intArrayTag.getName(), intArrayTag)
                .put(floatTag.getName(), floatTag)
                .put(longTag.getName(), longTag)
                .put(longArrayTag.getName(), longArrayTag)
                .put(doubleTag.getName(), doubleTag)
                .put(stringTag.getName(), stringTag)
                .put(listTag.getName(), listTag)
                .build());

        StringWriter sw = new StringWriter();
        XMLTransformer.toXML(compoundTag, sw);
        String xml = sw.toString();

        System.out.println(xml);

        Tag tag = XMLTransformer.fromXML(new StringReader(xml));

        assertEquals("tag0", tag.getName());
        assertEquals("tag1", ((CompoundTag) tag).getTag("tag1").getName());
        assertEquals(1, ((ByteTag) ((CompoundTag) tag).getTag("tag1")).getValue());
        assertEquals("tag2", ((CompoundTag) tag).getTag("tag2").getName());
        assertEquals("short", new String(((ByteArrayTag) ((CompoundTag) tag).getTag("tag2")).getValue(), Charsets.UTF_8));
        assertEquals("tag3", ((CompoundTag) tag).getTag("tag3").getName());
        assertEquals("Very long text which is certain to be shorter when compressed. Another sentence is apparently needed to bring it over the edge.", new String(((ByteArrayTag) ((CompoundTag) tag).getTag("tag3")).getValue(), Charsets.UTF_8));
        assertEquals("tag4", ((CompoundTag) tag).getTag("tag4").getName());
        assertEquals(2, ((ShortTag) ((CompoundTag) tag).getTag("tag4")).getValue());
        assertEquals("tag5", ((CompoundTag) tag).getTag("tag5").getName());
        assertEquals(3, ((IntTag) ((CompoundTag) tag).getTag("tag5")).getValue());
        assertEquals("tag6", ((CompoundTag) tag).getTag("tag6").getName());
        assertArrayEquals(new int[] {1, 1, 2, 3, 5, 8, 13}, ((IntArrayTag) ((CompoundTag) tag).getTag("tag6")).getValue());
        assertEquals("tag7", ((CompoundTag) tag).getTag("tag7").getName());
        assertEquals(4.5f, ((FloatTag) ((CompoundTag) tag).getTag("tag7")).getValue(), 0f);
        assertEquals("tag8", ((CompoundTag) tag).getTag("tag8").getName());
        assertEquals(6L, ((LongTag) ((CompoundTag) tag).getTag("tag8")).getValue());
        assertEquals("tag9", ((CompoundTag) tag).getTag("tag9").getName());
        assertArrayEquals(new long[] {8L, 13L, 21L, 34L, 55L}, ((LongArrayTag) ((CompoundTag) tag).getTag("tag9")).getValue());
        assertEquals("tag10", ((CompoundTag) tag).getTag("tag10").getName());
        assertEquals(7.8, ((DoubleTag) ((CompoundTag) tag).getTag("tag10")).getValue(), 0.0);
        assertEquals("tag11", ((CompoundTag) tag).getTag("tag11").getName());
        assertEquals("Are < & > properly escaped? How about\nnewlines?", ((StringTag) ((CompoundTag) tag).getTag("tag11")).getValue());
        assertEquals("tag12", ((CompoundTag) tag).getTag("tag12").getName());
        assertEquals(3, ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().size());
        assertEquals("tag13", ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().get(0).getName());
        assertEquals("One", ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().get(0).getValue());
        assertEquals("tag14", ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().get(1).getName());
        assertEquals("Two", ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().get(1).getValue());
        assertEquals("tag15", ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().get(2).getName());
        assertEquals("Three", ((ListTag<StringTag>) ((CompoundTag) tag).getTag("tag12")).getValue().get(2).getValue());
    }
}