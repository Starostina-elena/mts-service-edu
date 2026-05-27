package ru.aigul.mts_service.jca;

public class TaigaConnectionFactoryImpl implements TaigaConnectionFactory {

    @Override
    public TaigaConnection getConnection() {
        return new TaigaConnectionImpl();
    }
}
