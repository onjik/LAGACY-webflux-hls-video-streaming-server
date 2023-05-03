package com.oj.videostreamingserver.learning_test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Disabled
public class MonoThreadTest {
    @Test
    void test() {
        Mono<String> stringMono = Mono.fromCallable(() -> {
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + "| i = " + i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            return "hello";
        }).then(Mono.fromCallable(() -> {
            for (int i = 10; i < 20; i++) {
                System.out.println(Thread.currentThread().getName() + "| i = " + i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            return "world";
        })).subscribeOn(Schedulers.boundedElastic());

        System.out.println("dkdk");
        Disposable subscribe = stringMono.subscribe(System.out::println);
        System.out.println("dkdk");
        System.out.println("subscribe = " + subscribe);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
        subscribe.dispose();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        System.out.println(subscribe.isDisposed());
    }

    @Test
    void test2(){
        //오퍼레이터 체인 안에서 subscribe 하면?
        Mono<String> rootChain = Mono.just("rootChain")
                .flatMap(s -> {
                    Mono<String> childChain = Mono.fromCallable(() -> {
                        for (int i = 0; i < 10; i++) {
                            System.out.println(Thread.currentThread().getName() + "| children " + i);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return "hello";
                    }).subscribeOn(Schedulers.boundedElastic());
                    childChain.subscribe(System.out::println);
                    return Mono.just(s);
                })
                .flatMap(s -> {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(Thread.currentThread().getName() + "| root " + i);
                    }
                    return Mono.just(s);
                });

        String block = rootChain.block();
        System.out.println("block = " + block);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
    }


    @Test
    void test3(){
        Flux<String> stringFlux = Flux.just("1")
                .flatMap(item -> Mono.just(item).doOnNext(s -> System.out.println(Thread.currentThread().getName() + " : " + s)).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(item -> Mono.just(item).doOnNext(s -> System.out.println(Thread.currentThread().getName() + " : " + s))) // 기본 스케줄러 사용
                .flatMap(item -> Mono.just(item).doOnNext(s -> System.out.println(Thread.currentThread().getName() + " : " + s)).subscribeOn(Schedulers.boundedElastic()));// boundedElastic 스케줄러 사용

        stringFlux.blockLast();
    }

    @Test
    void 쓰레드_분할(){
        Mono<String> he = Mono.just(1)
                .doOnNext(i -> System.out.println(Thread.currentThread().getName() + " : " + i))
                .flatMap(i -> {
                    return Mono.just("he")
                            .flatMap(s -> {
                                System.out.println(Thread.currentThread().getName() + " : " + s);
                                return Mono.just(s);
                            }).subscribeOn(Schedulers.boundedElastic())
                            .publishOn(Schedulers.parallel());
                })
                .doOnNext(s -> System.out.println(Thread.currentThread().getName() + " : " + s));
        String block = he.block();
        System.out.println("block = " + block);
    }
}
