package com.oj.videostreamingserver.domain.vod.component;

import java.util.UUID;

public interface EncodingResultNotifier {

    void notifyComplete(UUID videoId, EncodingChannel.Type type);

    void notifyError(UUID videoId, EncodingChannel.Type type);
}
