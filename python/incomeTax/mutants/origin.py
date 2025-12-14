'''
计算收入税
'''
def income_tax(income):
    """
    Calculate income tax based on different income brackets.

    Args:
        income (float): The total income amount to calculate tax for

    Returns:
        float: The calculated tax amount based on the income brackets

    """
    if income <= 20000:
        tax = 0
    elif income <= 30000:
        tax = 0.12 * (income - 20000)
    elif income <= 40000:
        tax = 1200 + 0.14 * (income - 30000)
    elif income <= 50000:
        tax = 2600 + 0.17 * (income - 40000)
    elif income <= 500000:
        tax = 0.15 * income
    else:
        tax = 0.165 * income
    return tax
