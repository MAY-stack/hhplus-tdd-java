# 동시성 제어

- **동시성 문제**를 해결하기 위해 동시에 요청을 받았을 때 어떻게 처리 되도록 할 지를 제어하는 것
    
    > 💡**동시성 문제**
    여러 개의 작업(스레드 또는 프로세스)이 **동시에 실행**되면서 **공유 자원**(예: 변수, 데이터 구조, 파일 등)에 접근하거나 수정하려고 할 때 발생하는 문제.
    예)[ID가 ‘1’ 인 사람에 대한 포인트 충전 요청이 동시에 들어왔을 경우](#한-가지-요청은-여러-동작을-통해서-처리-된다)
    > 

### 작업의 처리 방식(멀티 스레드 ≠ 비동기)

- 싱글 스레드 / 멀티 스레드
- 동기 / 비동기
    
    ![image](https://github.com/user-attachments/assets/184795a4-f34b-4085-bc33-10a15aa06334)
    
    

### Spring Boot는 어떻게 여러 사용자의 요청을 처리 하는가?

- 스프링은 기본적으로 멀티 스레드, 동기 방식으로 동작한다.
1. Spring Boot가 실행될 때, 내장 Tomcat(Servlet Container)에서 ThreadPool을 생성한다.
2. 사용자의 요청(HttpServletRequest) 하나 마다 Thread Pool에서 Thread를 하나씩 할당한다.
3. 해당 Thread에서 Spring Boot에 작성한 Dispatcher Servlet을 거쳐 유저 요청을 처리한다.
4. 작업을 모두 수행하고 나면 Thread는 Thread Pool로 반환된다.

### 한 가지 요청은 여러 동작을 통해서 처리 된다.

- ID가 ‘1’ 인 사람에 대한 포인트 충전 요청이 동시에 들어왔을 경우,
    
  ![1](https://github.com/user-attachments/assets/a8c658cb-6a16-48bf-9909-f1d4a86d3010)
    
    ⇒ 실제로는 총 300 포인트 충전 요청이 들어왔지만 100 포인트 만 충전이 됐다.
    
- 동시성 문제가 생기기 않도록 하기 위해서 적절한 처리를 해주어야 한다. : 동시성 제어

# 동시성 제어 방식 및 구현

### Synchronized

- synchronized 키워드를 사용해 해당 블럭의 액세스를 동기화할 수 있다.
즉, synchronized 가 선언된 블럭 에는 동시에 하나의 스레드만 접근할 수 있도록 하는 것이다.
    
    ⇒ 이것은 하나의 프로세스에서만 보장되는 특성으로 서버가 여러 대일 때는 동시성 문제에서 자유로울 수 없다.
    
    - 이 방법을 포인트 충전 서비스에 적용한다면,
        
        ![2](https://github.com/user-attachments/assets/fda40576-5ae3-4180-a411-1fabe22d091f)
        
        ⇒한번에 하나의 요청만 처리해서 총 300 포인트의 충전이 처리 되었다.
        
    - 하나의 아이디가 아니라 여러 사람의 아이디로 요청이 들어온다면,
        
        ![3](https://github.com/user-attachments/assets/788183bd-d012-4c7f-9834-fd76890ea762)
        
        ⇒운이 좋으면 제일 먼저 처리 되겠지만, 운이 나쁘다면 ID2와 같이 다른 사용자의 요청을 모두 기다린 후에 내 요청이 처리 될 수도 있다. 
        다른 사용자의 요청에 대한 처리는 내 요청에 영향을 주지 않는데 불필요한 대기 시간이 발생 하는 것이다.
        
    - 위와 같은 이유로 Synchronized 방식은 적합하지 않았다.

### ReentrantLock

- Syncronized는 jvm 수준에서 lock이 관리 되고 블럭과 메서드 범위에 동기화를 적용할 수 있지만, ReentrantLock은 명시적으로 lock과 unlock을 제공하여 조금 더 세밀하게 동기화를 제어할 수 있다고 한다.
- 또한 ReentrantLock은 생성 시에 설정을 통해 공
    - 단순히 하나의 Lock을 생성해서 포인트 충전 메서드를 호출 할 때 lock을, 작업이 종료된 후에 unlock을 한다면,,, Synchronized와 다를 바가 없다.
    - 아이디 별로 lock을 관리하면 동시성 문제를 해결하면서 다른 ID 간의 요청은 병렬로 처리 될 수 있다.
    
      ![4](https://github.com/user-attachments/assets/9769e4c9-36e3-4ef9-8f80-0a98101c696d))
    
    - 이 방법에서도 Lock을 관리하는 Map에 여러 Thread가 동시에 같은 Lock에 접근하면서 
    마찬가지로 동시성 문제인 `ConcurrentModificationException` 이 발생할 수 있다.
        
        ⇒ 이 때문에 **Thread-safety**한 Map인 ContcurrentHashMap을 사용해야 했다.
