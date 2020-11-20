package org.springframework.content.renditions.renderers;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

public class JpegToPngRenditionProviderTest {

	private RenditionProvider service;

	@Before
	public void setUp() {
		service = new JpegToPngRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service.consumes(), is("image/jpeg"));
		assertThat(Arrays.asList(service.produces()), hasItems("image/png"));
	}

	@Test
	public void testConvert() throws Exception {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample.jpeg"), "image/png");

		assertThat(converted.available(), is(greaterThan(0)));
		assertThat(((ObservableInputStream)converted).getObservers(), hasItem(is(instanceOf(FileRemover.class))));

		BufferedImage expectedImage = ImageIO.read(this.getClass().getResourceAsStream("/sample.png"));
		byte[] expectedRastaData = ((DataBufferByte) expectedImage.getData().getDataBuffer()).getData();

		BufferedImage actualImage = ImageIO.read(converted);
		byte[] actualRastaData = ((DataBufferByte) actualImage.getData().getDataBuffer()).getData();

        assertThat(expectedRastaData, is(actualRastaData));
	}
}
