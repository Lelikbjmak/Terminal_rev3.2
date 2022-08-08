package Termminal;

public enum Currency_rate {
    USD_to_EURO(0.99),     // 1usd = 0.99 euro
    EURO_to_USD(1.038),    // same to previous but vice versa
    BYN_to_USD(0.3844),      // sale
    USD_to_BYN(2.5741),       // buy    FROM THE FACE OF BANK!
    BYN_to_EURO(0.3821),     // sale
    EURO_to_BYN(2.61),     // buy
    BYN_to_RUB(22.53),      // on 100 rub   (sale)
    RUB_to_BYN(0.0444),       // buy
    RUB_to_EURO(0.0171),
    RUB_to_USD(0.0172),
    USD_to_RUB(58.2133),
    EURO_to_RUB(58.3981);
    final double rate;
    Currency_rate(double num){
        this.rate = num;
    }
}
