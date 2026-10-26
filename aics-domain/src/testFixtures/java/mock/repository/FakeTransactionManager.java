package mock.repository;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

// Fake 리포지토리를 쓰는 순수 단위 테스트에서 TransactionTemplate을 생성자로 받는
// CommandService를 구성할 때 쓰는 더미 트랜잭션 매니저. 실제 트랜잭션 경계는 없고
// 콜백을 그대로 실행만 한다.
public class FakeTransactionManager implements PlatformTransactionManager {

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
    }
}
