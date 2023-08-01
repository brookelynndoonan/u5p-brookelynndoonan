#!/bin/bash
set -eo pipefail

source ./setupEnvironment.sh

if [ -z "$GITHUB_USERNAME" ] || [ "$GITHUB_USERNAME" == "yourusernameinlowercase" ] ; then
  echo "Your environment variable GITHUB_USERNAME is not properly configured.  Make sure that you have set it properly in setupEnvironment.sh"
  exit 1
fi



TABLE_NAME="Customer"
echo "Checking Table $TABLE_NAME"
{
  aws dynamodb delete-table --table-name $TABLE_NAME > /dev/null 2>&1 &&
  echo "Deleting Table $TABLE_NAME" &&
  echo "This may take 2-3 minutes...  But if takes more than 5 minutes then it may have failed. Check your DynamoDB tables on the AWS UI for errors." &&
  ( aws dynamodb wait table-not-exists --table-name $TABLE_NAME ||
   echo "Table may have not deleted. Check your DynamoDB tables on the AWS UI for errors." )
} || {
  echo "Table $TABLE_NAME does not exist"
} ;
echo ""


TABLE_NAME="Referral"
echo "Checking Table $TABLE_NAME"
{
  aws dynamodb delete-table --table-name $TABLE_NAME > /dev/null 2>&1 &&
  echo "Deleting Table $TABLE_NAME" &&
  echo "This may take 2-3 minutes...  But if takes more than 5 minutes then it may have failed. Check your DynamoDB tables on the AWS UI for errors." &&
  ( aws dynamodb wait table-not-exists --table-name $TABLE_NAME ||
   echo "Table may have not deleted. Check your DynamoDB tables on the AWS UI for errors." )
} || {
  echo "Table $TABLE_NAME does not exist"
} ;
echo ""


TABLE_STACK=referral-table
echo "Deleting Table Stack $TABLE_STACK"
echo "This may take 5-10 minutes...  But if takes more than 15 minutes then it may have failed. Check your CloudFormation Stack on the AWS UI for errors."
aws cloudformation delete-stack --stack-name $TABLE_STACK
aws cloudformation wait stack-delete-complete --stack-name $TABLE_STACK