/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.web.util;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * @author Arjen Poutsma
 * @author Phillip Webb
 */
public class UriComponentsTests {

    @Test
    public void encode() {
        UriComponents uriComponents = UriComponentsBuilder.fromPath("/hotel list").build();
        UriComponents encoded = uriComponents.encode();
        assertEquals("/hotel%20list", encoded.getPath());
    }

    @Test
    public void toUriEncoded() throws URISyntaxException {
        UriComponents uriComponents =
                UriComponentsBuilder.fromUriString("http://example.com/hotel list/Z\u00fcrich").build();
        assertEquals(new URI("http://example.com/hotel%20list/Z%C3%BCrich"), uriComponents.encode().toUri());
    }

    @Test
    public void toUriNotEncoded() throws URISyntaxException {
        UriComponents uriComponents =
                UriComponentsBuilder.fromUriString("http://example.com/hotel list/Z\u00fcrich").build();
        assertEquals(new URI("http://example.com/hotel%20list/Z\u00fcrich"), uriComponents.toUri());
    }

    @Test
    public void toUriAlreadyEncoded() throws URISyntaxException {
        UriComponents uriComponents =
                UriComponentsBuilder.fromUriString("http://example.com/hotel%20list/Z%C3%BCrich").build(true);
        UriComponents encoded = uriComponents.encode();
        assertEquals(new URI("http://example.com/hotel%20list/Z%C3%BCrich"), encoded.toUri());
    }

    @Test
    public void toUriWithIpv6HostAlreadyEncoded() throws URISyntaxException {
        UriComponents uriComponents = UriComponentsBuilder
                .fromUriString("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich").build(true);
        UriComponents encoded = uriComponents.encode();
        assertEquals(new URI("http://[1abc:2abc:3abc::5ABC:6abc]:8080/hotel%20list/Z%C3%BCrich"), encoded.toUri());
    }

    @Test
    public void expand() {
        UriComponents uriComponents =
                UriComponentsBuilder.fromUriString("http://example.com").path("/{foo} {bar}").build();
        uriComponents = uriComponents.expand("1 2", "3 4");
        assertEquals("/1 2 3 4", uriComponents.getPath());
        assertEquals("http://example.com/1 2 3 4", uriComponents.toUriString());
    }

    // SPR-13311

    @Test
    public void expandWithRegexVar() {
        String template = "/myurl/{name:[a-z]{1,5}}/show";
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(template).build();
        uriComponents = uriComponents.expand(Collections.singletonMap("name", "test"));
        assertEquals("/myurl/test/show", uriComponents.getPath());
    }

    // SPR-12123

    @Test
    public void port() {
        UriComponents uri1 = fromUriString("http://example.com:8080/bar").build();
        UriComponents uri2 = fromUriString("http://example.com/bar").port(8080).build();
        UriComponents uri3 = fromUriString("http://example.com/bar").port("{port}").build().expand(8080);
        UriComponents uri4 = fromUriString("http://example.com/bar").port("808{digit}").build().expand(0);
        assertEquals(8080, uri1.getPort());
        assertEquals("http://example.com:8080/bar", uri1.toUriString());
        assertEquals(8080, uri2.getPort());
        assertEquals("http://example.com:8080/bar", uri2.toUriString());
        assertEquals(8080, uri3.getPort());
        assertEquals("http://example.com:8080/bar", uri3.toUriString());
        assertEquals(8080, uri4.getPort());
        assertEquals("http://example.com:8080/bar", uri4.toUriString());
    }

    @Test(expected = IllegalStateException.class)
    public void expandEncoded() {
        UriComponentsBuilder.fromPath("/{foo}").build().encode().expand("bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCharacters() {
        UriComponentsBuilder.fromPath("/{foo}").build(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidEncodedSequence() {
        UriComponentsBuilder.fromPath("/fo%2o").build(true);
    }

    @Test
    public void normalize() {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString("http://example.com/foo/../bar").build();
        assertEquals("http://example.com/bar", uriComponents.normalize().toString());
    }

    @Test
    public void serializable() throws Exception {
        UriComponents uriComponents =
                UriComponentsBuilder.fromUriString("http://example.com").path("/{foo}").query("bar={baz}").build();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(uriComponents);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        UriComponents readObject = (UriComponents) ois.readObject();
        assertThat(uriComponents.toString(), equalTo(readObject.toString()));
    }

    @Test
    public void copyToUriComponentsBuilder() {
        UriComponents source = UriComponentsBuilder.fromPath("/foo/bar").pathSegment("ba/z").build();
        UriComponentsBuilder targetBuilder = UriComponentsBuilder.newInstance();
        source.copyToUriComponentsBuilder(targetBuilder);
        UriComponents result = targetBuilder.build().encode();
        assertEquals("/foo/bar/ba%2Fz", result.getPath());
        assertEquals(Arrays.asList("foo", "bar", "ba%2Fz"), result.getPathSegments());
    }

    @Test
    public void equalsHierarchicalUriComponents() throws Exception {
        UriComponents uriComponents1 =
                UriComponentsBuilder.fromUriString("http://example.com").path("/{foo}").query("bar={baz}").build();
        UriComponents uriComponents2 =
                UriComponentsBuilder.fromUriString("http://example.com").path("/{foo}").query("bar={baz}").build();
        UriComponents uriComponents3 =
                UriComponentsBuilder.fromUriString("http://example.com").path("/{foo}").query("bin={baz}").build();
        assertThat(uriComponents1, instanceOf(HierarchicalUriComponents.class));
        assertThat(uriComponents1, equalTo(uriComponents1));
        assertThat(uriComponents1, equalTo(uriComponents2));
        assertThat(uriComponents1, not(equalTo(uriComponents3)));
    }

    @Test
    public void equalsOpaqueUriComponents() throws Exception {
        UriComponents uriComponents1 = UriComponentsBuilder.fromUriString("http:example.com/foo/bar").build();
        UriComponents uriComponents2 = UriComponentsBuilder.fromUriString("http:example.com/foo/bar").build();
        UriComponents uriComponents3 = UriComponentsBuilder.fromUriString("http:example.com/foo/bin").build();
        assertThat(uriComponents1, instanceOf(OpaqueUriComponents.class));
        assertThat(uriComponents1, equalTo(uriComponents1));
        assertThat(uriComponents1, equalTo(uriComponents2));
        assertThat(uriComponents1, not(equalTo(uriComponents3)));
    }

}
