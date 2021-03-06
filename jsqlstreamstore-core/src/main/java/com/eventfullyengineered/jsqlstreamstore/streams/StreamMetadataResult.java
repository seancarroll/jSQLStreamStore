package com.eventfullyengineered.jsqlstreamstore.streams;

import com.google.common.base.MoreObjects;

/**
 *
 *
 */
public class StreamMetadataResult {

	/**
	 * The stream ID
	 */
	private final String streamId;

	/**
	 * The version of the metadata stream. Can be used for concurrency control
	 */
	private final long metadataStreamVersion;

	private final Integer maxAge;

	private final Long maxCount;

	private final String metadata;

    public StreamMetadataResult(String streamId, int metadataStreamVersion) {
        this(streamId, metadataStreamVersion, null);
    }


	public StreamMetadataResult(String streamId, int metadataStreamVersion, String metadataJson) {
	    this(streamId, metadataStreamVersion, null, null, metadataJson);
	}

	/**
	 *
	 * @param streamId The stream ID
	 * @param metadataStreamVersion The verson of the metadata stream
	 * @param maxAge The max age of messages in the stream
	 * @param maxCount The max count of message in the stream
	 * @param metadata Custom metadata serialized as JSON
	 */
    public StreamMetadataResult(String streamId,
                                long metadataStreamVersion,
                                Integer maxAge,
                                Long maxCount,
                                String metadata) {
        this.streamId = streamId;
        this.metadataStreamVersion = metadataStreamVersion;
        this.maxAge = maxAge;
        this.maxCount = maxCount;
        this.metadata = metadata;
    }

    public String getStreamId() {
        return streamId;
    }

    public long getMetadataStreamVersion() {
        return metadataStreamVersion;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public Long getMaxCount() {
        return maxCount;
    }

    public String getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("streamId", getStreamId())
                .add("maxAge", getMaxAge())
                .add("maxCount", getMaxCount())
                .add("metadataStreamVersion", getMetadataStreamVersion())
                .toString();
    }
}
