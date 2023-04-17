package com.oj.videostreamingserver.domain.vod.api;

//@RestController
//@RequiredArgsConstructor
public class VodApi {
//
//    private final CryptoUtil cryptoUtil;
//
//    private final VideoFileService videoFileService;
//    private final TransactionalOperator transactionalOperator;
//
//    @PostMapping("/media")
//    public Mono<ServerResponse> postVideo(@RequestPart("video")Mono<FilePart> videoFilePart){
//        long channelId = 1; //임시로 구현
//        return videoFilePart
//                .log()
//                //컨텐츠 타입 체크
//                .filter(fp->fp.headers().getContentType() != null)
//                .filter(fp->fp.headers().getContentType().toString().startsWith("video/"))
//                .switchIfEmpty(Mono.error(new BadRequestException("invalid video")))
//                //트랜잭션 처리
//                .as(transactionalOperator::transactional)
//                //컨텐츠를 로컬 파일에 저장합니다.
//                .flatMap(filePart -> videoFileService.saveDraft(filePart,channelId)) //Mono<DraftVideo>
//                //확인 응답
//                .flatMap(draftVideo ->
//                        ServerResponse
//                                .ok().build()
////                                .contentType(MediaType.APPLICATION_JSON)
////                                .body(new OriginalVideoPostResponse(draftVideo.getId()), OriginalVideoPostResponse.class)
//                )
//                //예외처리
//                .onErrorResume(BadRequestException.class,e -> ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE))
//                .onErrorResume(InvalidTargetPathException.class,ErrorResponse::of)
//                .onErrorResume(Exception.class, ErrorResponse::of);
//    }
}
