'''
计算 mortgage rate
'''
def mortgage_rate(house_value):
    """
    计算 mortgage rate
    Args:
        house_value (float): The value of the house
    Returns:
        float: The mortgage rate for the given house value
    """
    prime_rate = 1  # pragma: no mutate
    if house_value >= 205000:
        mortgage_rate = prime_rate + 1.75
    else:
        mortgage_rate = prime_rate + 1.27
    return mortgage_rate * house_value